package com.projectClient.disc_mapper_client.functionality;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.attribute.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.projectClient.disc_mapper_client.objects.DriveDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@EnableAsync
public class DriveFuncs {

    private static final long ONE_GB_IN_BYTES = 1024L * 1024 * 1024;
    private static final long ONE_MB_IN_BYTES = 1024L * 1024;

    private static final AtomicInteger totalDirs = new AtomicInteger(0);
    private static final AtomicInteger processedDirs = new AtomicInteger(0);

    @Autowired
    @Qualifier("driveExecutor")
    private Executor driveExecutor;


    public List<DriveDataDTO> getDrivesData() {
        List<String> roots = listDrives();

        return roots.stream()
                .map(root -> {
                    DriveDataDTO dto = new DriveDataDTO();
                    dto.setDriveRoot(root);
                    dto.setTotalStorage(storage(root));
                    dto.setAvailableStorage(freeStorage(root));
                    return dto;
                })
                .collect(Collectors.toList()
                );
    }

    public List<String> listDrives() {

        List<String> driveList = new ArrayList<>();
        File[] drives = File.listRoots();

        for (File root : drives) {
            driveList.add(root.getAbsolutePath());
        }

        return driveList;
    }

    public String storage(String path) {
        return formatStorageSpace(path, false);
    }

    public String freeStorage(String path) {
        return formatStorageSpace(path, true);
    }

    private String formatStorageSpace(String path, boolean isFreeSpace) {

        File drive = new File(path);

        if (!drive.exists() || !drive.isDirectory()) {
            return "Not accessible";
        }

        long spaceBytes = isFreeSpace ? drive.getUsableSpace() : drive.getTotalSpace();
        double spaceInGb = (double) spaceBytes / ONE_GB_IN_BYTES;

        if (spaceInGb >= 1.0) {
            return Math.round(spaceInGb * 100.0) / 100.0 + " GB";
        } else {
            double spaceInMb = (double) spaceBytes / ONE_MB_IN_BYTES;
            return Math.round(spaceInMb * 100.0) / 100.0 + " MB";
        }
    }


    public CompletableFuture<Map<String, Object>> mapDirectory(Path directory) {

        return CompletableFuture.supplyAsync(() -> {
            totalDirs.set(0);
            processedDirs.set(0);

            ForkJoinPool sharedExecutor = ForkJoinPool.commonPool();

            Thread progressThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    int t = totalDirs.get();
                    int p = processedDirs.get();

                    System.out.printf("\rMapping progress: %d/%d directories processed", p, t);

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                System.out.println();
            });

            progressThread.setDaemon(true);
            progressThread.start();

            try {
                return mapDirectoryInternal(directory, totalDirs, processedDirs, sharedExecutor);
            } catch (IOException e) {
                throw new CompletionException("Directory mapping failed", e);
            } finally {
                progressThread.interrupt();
            }
        }, driveExecutor);
    }

    private Map<String, Object> mapDirectoryInternal(Path directory,
                                                     AtomicInteger totalDirs,
                                                     AtomicInteger processedDirs,
                                                     ForkJoinPool sharedExecutor) throws IOException {

        totalDirs.incrementAndGet();
        Map<String, Object> result = new ConcurrentHashMap<>();

        if (!Files.isDirectory(directory)) {
            processedDirs.incrementAndGet();
            return result;
        }

        Queue<Map<String, Object>> fileQueue = new ConcurrentLinkedQueue<>();
        Map<String, Object> subDirectories = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        try (Stream<Path> pathStream = Files.list(directory)) {
            pathStream.forEach(filePath -> {
                futures.add(sharedExecutor.submit(() -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

                        if (attrs.isDirectory()) {
                            boolean skip = false;

                            try {
                                if (Files.isHidden(filePath)) skip = true;
                            } catch (IOException e) {
                                skip = true;
                            }

                            try {
                                Boolean isSystem = (Boolean) Files.getAttribute(filePath, "dos:system");
                                if (isSystem != null && isSystem) skip = true;
                            } catch (UnsupportedOperationException | IOException ignored) {
                                skip = true;
                            }

                            if (!skip) {
                                try {
                                    Map<String, Object> dirMap = mapDirectoryInternal(filePath, totalDirs, processedDirs, sharedExecutor);
                                    subDirectories.put(filePath.getFileName().toString(), dirMap);
                                } catch (IOException e) {
                                    System.err.println("Skipped directory (access denied): " + filePath);
                                }
                            }
                        } else {
                            try {
                                Map<String, Object> fileInfo = new ConcurrentHashMap<>();
                                fileInfo.put("name", filePath.getFileName().toString());
                                fileInfo.put("lastModified", attrs.lastModifiedTime().toMillis());
                                fileQueue.add(fileInfo);
                            } catch (Exception e) {
                                System.err.println("Error reading file: " + filePath);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error processing path: " + filePath + " — " + e.getMessage());
                    }
                }));
            });
        } catch (IOException e) {
            System.err.println("Cannot list directory: " + directory + " — " + e.getMessage());
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.err.println("Task execution error: " + e.getCause().getMessage());
            }
        }

        result.put("files", new ArrayList<>(fileQueue));
        result.put("directories", subDirectories);
        processedDirs.incrementAndGet();
        return result;
    }


}