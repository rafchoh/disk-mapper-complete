package com.project.disc_mapper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.disc_mapper.dto.entity.DriveFiles;
import com.project.disc_mapper.dto.entity.Drives;
import com.project.disc_mapper.repo.DrFilesRepo;
import com.project.disc_mapper.repo.DriveRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DrFilesService {

    private final Map<String, Double> progressMap = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, String> driveToUserMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> cancelMap = new ConcurrentHashMap<>();


    private static final int BATCH_SIZE = 100;
    private final AtomicInteger batchCounter = new AtomicInteger(0);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DrFilesRepo dfRepo;
    @Autowired
    private DriveRepo driveRepo;
    @Autowired
    private UserService userService;
    @Autowired
    private DriveService driveService;

    public Map<String, List<DriveFiles>> getFilesnFoldersByDriveId(Long driveId) {

        return dfRepo.findAllFilesnFoldersByDriveId(driveId)
                .stream()
                .collect(Collectors.groupingBy(
                        DriveFiles::getParentPath,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }


    @Async("importExecutor")
    @Transactional
    public CompletableFuture<Void> importFilesInBackground(String drivePath,
                                                           Long driveId) {

        String taskId = driveId.toString();
        cancelMap.put(taskId, new AtomicBoolean(false));

        try {
            Thread.sleep(100);

            if (!driveRepo.existsById(driveId)) {
                cleanupTask(taskId);
                throw new IllegalArgumentException("Drive with ID=" + driveId + " is not present!");
            }

            JsonNode root = new ObjectMapper().readTree(driveService.getJsonFromDbForDrive(driveId));
            long totalNodes = countNodes(root);
            AtomicLong processed = new AtomicLong(0);
            Path initialPath = Paths.get(drivePath);

            boolean completed = traverseAndPersist(root, initialPath, driveId, processed, totalNodes);

            if (completed) {
                System.out.println("\n✅ Import Complete!");
            } else {
                System.out.println("\n⚠️ Import Cancelled!");
                sendCancelledMessage(driveId);
            }

            if (batchCounter.get() > 0) {
                em.flush();
                em.clear();
                batchCounter.set(0);
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception ex) {
            cleanupTask(taskId);
            throw new RuntimeException("Error while importing: " + ex.getMessage(), ex);
        } finally {
            cleanupTask(taskId);
        }
    }

    private boolean traverseAndPersist(JsonNode node,
                                       Path path,
                                       Long driveId,
                                       AtomicLong processed,
                                       long totalNodes) {

        String taskId = driveId.toString();

        if (isCancelled(taskId)) {
            return false;
        }

        JsonNode filesNode = node.get("files");
        if (filesNode != null && filesNode.isArray()) {
            for (JsonNode f : filesNode) {
                if (isCancelled(taskId)) {
                    return false;
                }

                persistFile(
                        f.get("name").asText(),
                        f.get("lastModified").asLong(),
                        path,
                        driveId
                );

                long done = processed.incrementAndGet();
                sendProgress(driveId, done, totalNodes);
//                printProgress(done, totalNodes);
            }
        }

        JsonNode dirsNode = node.get("directories");
        if (dirsNode != null && dirsNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = dirsNode.fields();

            while (it.hasNext()) {
                if (isCancelled(taskId)) {
                    return false;
                }

                Map.Entry<String, JsonNode> entry = it.next();
                String dirName = entry.getKey();
                JsonNode sub = entry.getValue();

                Path nextPath = path.resolve(dirName);

                persistDirectory(dirName, nextPath, driveId);
                if (!traverseAndPersist(sub, nextPath, driveId, processed, totalNodes)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void persistDirectory(String name,
                                  Path fullPath,
                                  Long driveId) {

        String parentPathStr = fullPath.getParent() != null
                ? fullPath.getParent().toString()
                : "";

        Drives driveRef = em.getReference(Drives.class, driveId);

        DriveFiles dir = DriveFiles.builder()
                .fileName(name)
                .filePath(fullPath.toString())
                .parentPath(parentPathStr)
                .isDirectory(true)
                .lastModified(null)
                .drive(driveRef)
                .build();

        em.persist(dir);

        if (batchCounter.incrementAndGet() % BATCH_SIZE == 0) {
            em.flush();
            em.clear();
            batchCounter.set(0);
        }
    }


    private void persistFile(String name,
                             long lastModified,
                             Path parentPath,
                             Long driveId) {

        Path fullPath = parentPath.resolve(name);
        Drives driveRef = em.getReference(Drives.class, driveId);

        DriveFiles file = DriveFiles.builder()
                .fileName(name)
                .filePath(fullPath.toString())
                .parentPath(parentPath.toString())
                .isDirectory(false)
                .lastModified(Instant.ofEpochMilli(lastModified)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDateTime())
                .drive(driveRef)
                .build();

        em.persist(file);

        if (batchCounter.incrementAndGet() % BATCH_SIZE == 0) {
            em.flush();
            em.clear();
            batchCounter.set(0);
        }
    }

    private long countNodes(JsonNode node) {
        long count = 0;

        JsonNode files = node.get("files");
        if (files != null && files.isArray()) {
            count += files.size();
        }

        JsonNode dirs = node.get("directories");
        if (dirs != null && dirs.isObject()) {
            Iterator<JsonNode> it = dirs.elements();
            while (it.hasNext()) {
                JsonNode sub = it.next();
                count += countNodes(sub);
            }
        }

        return count;
    }

    private void printProgress(long done, long total) {
        int width = 50;
        double percent = (double) done / total;
        int filled = (int) (width * percent);

        StringBuilder bar = new StringBuilder();

        bar.append("\r[");
        for (int i = 0; i < filled; i++) {
            bar.append("=");
        }
        for (int i = filled; i < width; i++) {
            bar.append(" ");
        }
        bar.append("] ");
        bar.append(String.format("%3d%% (%d/%d)", (int) (percent * 100), done, total));

        System.out.print(bar);
    }


    public SseEmitter createProgressEmitter(Long driveId) {
        SseEmitter emitter = new SseEmitter(0L);

        if (userService.checkDriveProperty(driveId)) {
            String taskId = driveId.toString();

            emitters.put(taskId, emitter);
            driveToUserMap.put(taskId, userService.getAuthUsername());

            emitter.onCompletion(() -> {
                emitters.remove(taskId);
                progressMap.remove(taskId);
                driveToUserMap.remove(taskId);
            });
            emitter.onTimeout(() -> {
                emitters.remove(taskId);
                progressMap.remove(taskId);
                driveToUserMap.remove(taskId);
            });
            emitter.onError((e) -> {
                emitters.remove(taskId);
                progressMap.remove(taskId);
                driveToUserMap.remove(taskId);
            });

            if (progressMap.containsKey(taskId)) {
                try {
                    emitter.send(String.format("%.0f", progressMap.get(taskId)));
                } catch (IOException ignored) {

                }
            }
        }

        return emitter;
    }


    private void sendProgress(Long driveId, long done, long total) {

        String taskId = driveId.toString();
        double percent = (double) done / total * 100;
        progressMap.put(taskId, percent);

        SseEmitter emitter = emitters.get(taskId);

        if (emitter != null) {
            try {
                emitter.send(String.format("%.0f", percent));

                if (done >= total) {
                    emitter.complete();
                    emitters.remove(taskId);
                    progressMap.remove(taskId);
                }
            } catch (IOException e) {
                emitters.computeIfPresent(taskId, (key, emp) -> {
                    emitters.remove(taskId);
                    progressMap.remove(taskId);

                    return null;
                });
            }
            ;
        }
    }

    private void cleanupTask(String taskId) {
        cancelMap.remove(taskId);
        progressMap.remove(taskId);
        driveToUserMap.remove(taskId);

        SseEmitter emitter = emitters.remove(taskId);

        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }


    public Double getProgressValue(Long driveId) {
        if (driveId == null) {
            return 0d;
        }

        return progressMap.get(driveId.toString());
    }

    public boolean cancelImport(Long driveId) {
        String taskId = driveId.toString();
        AtomicBoolean cancelFlag = cancelMap.get(taskId);

        if (cancelFlag != null) {
            cancelFlag.set(true);

            dfRepo.deleteAllByDriveId(Long.parseLong(taskId));
            return true;
        }

        return false;
    }

    private void sendCancelledMessage(Long driveId) {
        String taskId = driveId.toString();
        SseEmitter emitter = emitters.get(taskId);

        if (emitter != null) {
            try {
                emitter.send("CANCELLED");
                emitter.complete();
            } catch (IOException ignore) {
            } finally {
                emitters.remove(taskId);
                progressMap.remove(taskId);
            }
        }
    }

    private boolean isCancelled(String taskId) {
        AtomicBoolean cancelFlag = cancelMap.get(taskId);

        return cancelFlag != null && cancelFlag.get();
    }

    public boolean isDriveMappingInProgressForUser(String username) {
        for (Map.Entry<String, String> entry : driveToUserMap.entrySet()) {
            String driveId = entry.getKey();
            String user = entry.getValue();
            Double progress = progressMap.get(driveId);

            if (user.equals(username) && progress != null && progress > 0 && progress < 100) {
                return true;
            }
        }
        return false;
    }
}