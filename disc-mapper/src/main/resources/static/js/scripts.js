document.addEventListener('DOMContentLoaded', function() {
    let modal = document.getElementById('myModal');
    let openModalButton = document.getElementById('showModal');
    let closeModalButton = document.getElementById('closeModal');

    if (modal && openModalButton && closeModalButton) {
        openModalButton.addEventListener('click', function () {
            modal.style.display = 'block';
        });

        closeModalButton.addEventListener('click', function () {
            modal.style.display = 'none';
        });
    }
});





document.addEventListener('DOMContentLoaded', function() {
    let modalBackground = document.getElementById("modal-background");
    let searchSidebar = document.getElementById('searchSidebar');
    let searchBtn = document.getElementById('searchBtn');
    let closeSearch = document.getElementById('closeSearch');

    if (modalBackground && searchBtn && searchSidebar && closeSearch) {
        searchBtn.addEventListener('click', function() {
            modalBackground.style.display = "block";
            searchSidebar.style.transform = 'translateX(100%)';
            document.body.style.overflow = "hidden";
        });

        closeSearch.addEventListener('click', function() {
            modalBackground.style.display = "none";
            searchSidebar.style.transform = 'translateX(-100%)';
            document.body.style.overflow = "auto";
        });

        modalBackground.addEventListener('click', function() {
            modalBackground.style.display = "none";
            searchSidebar.style.transform = 'translateX(-100%)';
            document.body.style.overflow = "auto";
        });
    }
});





let timeExpired = false;

function startTimer(initialSeconds) {
    let timerElement = document.getElementById('authTimer');
    let authButton = document.getElementById('authKeyButton');

    let remainingSeconds = initialSeconds;
    let timerInterval;

    function updateTimer() {
        let minutes = Math.floor(remainingSeconds / 60);
        let seconds = remainingSeconds % 60;
        timerElement.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;

        authButton.disabled = remainingSeconds <= 0;

        if (remainingSeconds <= 0) {
            clearInterval(timerInterval);
            timeExpired = true;
            return;
        }

        remainingSeconds--;
    }

    updateTimer();
    timerInterval = setInterval(updateTimer, 1000);
}





function validateForm(form) {
    let output = '';
    const errors = [];


    const validations = {
        full_name: /^[A-ZА-Я][a-zа-я]+(?:[-'\s][A-ZА-Я][a-zа-я]+)+$/u,
        username: /^[a-zA-Z0-9_-]{3,20}$/,
        password: /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$/,
        email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
        numbers: /^\d+$/
    };

    const fields = {
        fullName: form.querySelector('[name="fullName"], [th\\:field*="fullName"]'),
        username: form.querySelector('[name="username"], [th\\:field*="username"]'),
        password_reg: form.querySelector('[id="check_pass"]'),
        newPassword: form.querySelector('[name="newPassword"], [th\\:field*="newPassword"]'),
        retype_password: form.querySelector('[name="reTypePassword"], [th\\:field*="reTypePassword"]'),
        email: form.querySelector('[name="email"], [th\\:field*="email"]'),
        token_input: form.querySelector('[name="token"], [th\\:field*="token"]')
    };

    if (fields.fullName && fields.fullName.value.trim() && !validations.full_name.test(fields.fullName.value.trim())) {
        errors.push("Full name must include at least two words with capital letters!");
    }

    if (fields.username && fields.username.value.trim() && !validations.username.test(fields.username.value.trim())) {
        errors.push("Username must be 3-20 characters, latin script, numbers, _ or -");
    }

    const passwordFields = [
        fields.password_reg,
        fields.newPassword,
        fields.retype_password
    ].filter(Boolean);

    for (const field of passwordFields) {
        if (field.value.trim() && !validations.password.test(field.value.trim())) {
            errors.push("Password must contain 8+ characters, with uppercase, lowercase, and number");
            break;
        }
    }

    if (fields.newPassword && fields.retype_password &&
        fields.newPassword.value.trim() !== fields.retype_password.value.trim()) {
        errors.push("Passwords do not match!");
    }
    if (fields.password_reg && fields.retype_password &&
        fields.password_reg.value.trim() !== fields.retype_password.value.trim()) {
        errors.push("Passwords do not match!");
    }

    if (fields.email && fields.email.value.trim() && !validations.email.test(fields.email.value.trim())) {
        errors.push("Invalid email format!");
    }

    if (fields.token_input && fields.token_input.value.trim()) {
        if (!validations.numbers.test(fields.token_input.value.trim())) {
            errors.push("Token must contain numbers only!");
        }

        if (typeof timeExpired !== 'undefined' && timeExpired) {
            errors.push("Token has expired!");
        }
    }

    if (errors.length > 0) {
        output = errors.map(e => `• ${e}`).join('\n\n');
        alert(output);
        return false;
    }

    return true;
}





function togglePasswordVisibility(id) {
    let passwordInput = document.getElementById(id);
    passwordInput.type = (passwordInput.type === 'password') ? 'text' : 'password';
}





    document.addEventListener('DOMContentLoaded', function() {
        let isExternalCheckbox = document.querySelector('input[name="isExternal"]');

        if (isExternalCheckbox) {
            function togglePcDropdown() {
                let selectPcCont = document.querySelector('div[name="selectPcCont"]');
                let pcsDropdown = document.getElementById('pcsDropdown');

                if (!isExternalCheckbox.checked) {
                    selectPcCont.style.display = 'block';
                    pcsDropdown.disabled = false;
                } else {
                    selectPcCont.style.display = 'none';
                    pcsDropdown.disabled = true;
                }
            }

            togglePcDropdown();

            isExternalCheckbox.addEventListener('change', togglePcDropdown);
        }
    });





    document.addEventListener('DOMContentLoaded', function() {
        let isExternalCheckbox = document.querySelector('input[name="isExternal"]');

        if (isExternalCheckbox) {
            function togglePcDropdown() {
                let selectPcCont = document.querySelector('div[name="selectPcCont"]');
                let pcsDropdown = document.getElementById('pcsDropdown');

                if (!isExternalCheckbox.checked) {
                    selectPcCont.style.display = 'block';
                    pcsDropdown.disabled = false;
                } else {
                    selectPcCont.style.display = 'none';
                    pcsDropdown.disabled = true;
                }
            }

            togglePcDropdown();

            isExternalCheckbox.addEventListener('change', togglePcDropdown);
        }
    });







function confirmRemap(msg, opt) {
    if (msg == null || msg.trim() === '') {
        if (opt === 'remap') {
            return confirm('Are you sure you want to remap this drive?');
        } else if (opt === 'delete') {
            return confirm('Are you sure you want to delete this drive?');
        }
    } else {
        return confirm(msg);
    }
}





function checkAndInitProgressTracker(driveId) {
    let activeDriveId = driveId;

    const importInProgress = sessionStorage.getItem('importInProgress');
    const storedDriveId = sessionStorage.getItem('importDriveId');

    if (!activeDriveId && importInProgress === 'true' && storedDriveId) {
        activeDriveId = storedDriveId;
    }

    if (activeDriveId) {
        initProgressTracker(activeDriveId);
    }
}

function initProgressTracker(driveId) {
    if (!driveId) return;

    let currentEventSource = null;
    let importCancelled = false;

    sessionStorage.setItem('importInProgress', 'true');
    sessionStorage.setItem('importDriveId', driveId);


    const progressContainer = document.getElementById('progressModal');
    const progressInfo = document.getElementById("progressInfo");
    const progressText = document.getElementById("percent");
    const progressBar = document.getElementById("bar");
    const stopIcon = document.getElementById('stopImportIcon');
    const statusMessage = document.getElementById('statusMessage');
    const closeBtn = document.getElementById('closeProgressModal');
    const mapBtn = document.getElementById('mapDrive');


    if (!progressContainer || !progressText) {
        console.error('Progress elements not found');
        return;
    }

    progressContainer.style.display = 'block';
    stopIcon.style.display = 'block';
    importCancelled = false;

    sessionStorage.setItem('localDriveId', driveId);

    closeBtn.addEventListener('click', () => {
        if (currentEventSource) {
            currentEventSource.close();
            currentEventSource = null;
        }

        progressContainer.style.display = 'none';
    });

            function stopImport(driveId) {
                if (importCancelled) return;

                importCancelled = true;
                statusMessage.textContent = 'Stopping import...';

                if (stopIcon) stopIcon.style.display = 'none';

                fetch('/drive/create/' + driveId + '/cancel', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        statusMessage.textContent = 'Import cancelled. Cleaning up data...';

                        if (currentEventSource) {
                                    currentEventSource.close();
                                    currentEventSource = null;
                                }
                                progressContainer.style.display = 'none';

                                sessionStorage.removeItem('importInProgress');
                                sessionStorage.removeItem('importDriveId');
                                sessionStorage.removeItem('localDriveId');
                    } else {
                        statusMessage.textContent = 'Error cancelling import: ' + data.message;
                        console.error('Failed to cancel import:', data.message);
                    }
                })
                .catch(error => {
                    statusMessage.textContent = 'Error cancelling import.';
                    console.error('Error cancelling import:', error);
                });
            }

    if (stopIcon) {
        stopIcon.addEventListener('click', function() {
            stopImport(driveId);
        });
    }

    if (typeof EventSource === 'undefined') {
        return;
    }

    currentEventSource  = new EventSource(`/drive/create/${driveId}/progress`);

    currentEventSource.onopen = () => {
        console.log('SSE connection opened');
        statusMessage.textContent = 'Connected. Waiting for progress updates...';
    };

    currentEventSource.onmessage = e => {
        const data = e.data.trim();

        if (data === 'CANCELLED') {
            progressInfo.innerHTML = '<h5>❌ Import Cancelled</h5><p>All uploaded data has been removed.</p>';
            statusMessage.textContent = 'Import successfully cancelled.';

            sessionStorage.removeItem('importInProgress');
            sessionStorage.removeItem('importDriveId');
            sessionStorage.removeItem('localDriveId');

//            if (mapBtn) mapBtn.disabled = false;

            if (currentEventSource) {
                currentEventSource.close();
                currentEventSource = null;
            }

            setTimeout(() => {
                progressContainer.style.display = 'none';
            }, 3000);

            return;
        }

        const pct = Number(e.data);

        if (!isNaN(pct)) {
            progressBar.style.width = `${pct}%`;
            progressText.textContent = `${pct}%`;

            if (pct > 0 || pct < 100 ) {
                statusMessage.textContent = '';
            }

            if (pct >= 100) {
                progressInfo.innerHTML = '<h5>✅ Import Complete!</h5>';
                statusMessage.textContent = 'All files imported successfully.';

                if (stopIcon) stopIcon.style.display = 'none';

                sessionStorage.removeItem('importInProgress');
                sessionStorage.removeItem('importDriveId');
                sessionStorage.removeItem('localDriveId');

//                if (mapBtn) mapBtn.disabled = false;

                if (currentEventSource) {
                    currentEventSource.close();
                    currentEventSource = null;
                }

                if (window.location.pathname === '/drive/' + driveId) {
                    setTimeout( () => {
                        location.reload();
                    }, 500);
                }

                setTimeout(() => {
                    progressContainer.style.display = 'none';
                }, 5000);
            }
        }
    };

    currentEventSource.onerror = err => {
        if (sessionStorage.getItem('importInProgress') !== 'true') {
            if (currentEventSource) {
                currentEventSource.close();
                currentEventSource = null;
            }

            let modal = document.getElementById('progressModal');
            if (modal) modal.style.display = 'none';

            return;
        }

        console.error('SSE error:', err);

        if (!importCancelled) {
            statusMessage.textContent = 'Connection error. Reconnecting...';

            if (currentEventSource) {
                currentEventSource.close();
                currentEventSource = null;
            }

            setTimeout(() => {
                if (!importCancelled && sessionStorage.getItem('importInProgress') === 'true') {
                    console.log('Attempting to reconnect...');
                    initProgressTracker(driveId);
                }
            } , 5000);
        } else {
            sessionStorage.removeItem('importInProgress');
            sessionStorage.removeItem('importDriveId');
            sessionStorage.removeItem('localDriveId');
            localStorage.removeItem('localDriveId');
        }

//        if (!checkEnd) {}
//            progressInfo.textContent = '❌ Error while tracking progress!';
//        } else {
//            localStorage.removeItem('localDriveId');
//        }

//        if (mapBtn) mapBtn.disabled = false;
    };
}





function snClearAll() {
  if (window.currentEventSource) {
    window.currentEventSource.close();
    window.currentEventSource = null;
  }

  let modal = document.getElementById('progressModal');
  if (modal) modal.style.display = 'none';

  sessionStorage.removeItem('importInProgress');
  sessionStorage.removeItem('importDriveId');
  sessionStorage.removeItem('localDriveId');
  localStorage.removeItem('localDriveId');
}





function toggleFolder(header) {
       let folder = header.closest('.folder');
       folder.classList.toggle('collapsed');
}

document.addEventListener('DOMContentLoaded', function() {
     let params = new URLSearchParams(window.location.search);
     let fileId = params.get('fileId');

     let iconMap = {
         'js': '🟨', 'java': '☕', 'html': '🌐', 'css': '🎨', 'json': '📋', 'xml': '📄', 'txt': '📝',
         'pdf': '📕', 'doc': '📘', 'docx': '📘', 'jpg': '🖼️', 'jpeg': '🖼️', 'png': '🖼️', 'gif': '🖼️',
         'md': '📖', 'csv': '📊', 'xls': '📊', 'xlsx': '📊', 'ppt': '📑', 'pptx': '📑', 'zip': '🗜️',
         'rar': '🗜️', 'exe': '⚙️', 'bat': '⚙️', 'sh': '⚙️', 'mp3': '🎵', 'mp4': '🎬', 'avi': '🎬'
     };

         document.querySelectorAll('.file-item-name').forEach(function(fileElement) {
            const filename = fileElement.textContent;
            const ext = filename.split('.').pop().toLowerCase();
            const iconElement = fileElement.previousElementSibling;

            iconElement.textContent = iconMap[ext] || '📄';
            iconElement.title = `.${ext}`;
        });

     let folders = document.querySelectorAll('.folder');
        folders.forEach( (folder, index) => {
            if (index > 0) {
                folder.classList.add('collapsed');
            }
     });


     if (fileId) {
        let targetId = '№' + fileId;
        let targetEl = document.getElementById(targetId);
        if (!targetEl) return;

        function ensureFolderOpen(headerEl) {
          let folderDiv = headerEl.parentElement;
          let contentDiv = headerEl.nextElementSibling;
          if (contentDiv && getComputedStyle(contentDiv).display === 'none') {
            toggleFolder(headerEl);
          }
        }

        let currentFolder = targetEl.closest('.folder');
          while (currentFolder) {
            let header = currentFolder.querySelector(':scope > .folder-header');
            if (header) {
              ensureFolderOpen(header);
            }
            currentFolder = currentFolder.parentElement.closest('.folder');
        }

        document.querySelectorAll('.folder-header').forEach(header => {
          let parentFolder = header.parentElement;
          if (!parentFolder.parentElement.closest('.folder')) {
            ensureFolderOpen(header);
          }
        });

        targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        targetEl.classList.add('highlighted');
     }
});





document.addEventListener('DOMContentLoaded', function() {
    let searchTypeList = document.getElementsByName('searchType');
    let driveTypeDiv = document.getElementById('driveTypeDiv');
    let folderTypeDiv = document.getElementById('folderTypeDiv');
    let dateTypeDiv = document.getElementById('dateTypeDiv');
    let pcSearch = document.getElementById('pc-search');
    let driveSearch = document.getElementById('drive-search');
    let filesSearch = document.getElementById('files-search');

    let inCurrentDriveLabel = document.getElementById('currentDriveCheck');
    let isCurrentDrive = document.getElementById('currentDrive');

    if (searchTypeList && driveTypeDiv && folderTypeDiv && dateTypeDiv && pcSearch && driveSearch && filesSearch) {

        updateFilterVisibility();

        searchTypeList.forEach(radio => {
            radio.addEventListener('change', updateFilterVisibility);
        });

        function updateFilterVisibility() {
            driveTypeDiv.style.display = 'none';
            folderTypeDiv.style.display = 'none';
            dateTypeDiv.style.display = 'none';

            if (inCurrentDriveLabel) {
                inCurrentDriveLabel.style.display = 'none';
                inCurrentDriveLabel.disabled = true;
            }

            if (pcSearch.checked) {
                dateTypeDiv.style.display = 'block';
            }
            else if (driveSearch.checked) {
                driveTypeDiv.style.display = 'block';
                dateTypeDiv.style.display = 'block';
            }
            else if (filesSearch.checked) {
                driveTypeDiv.style.display = 'block';

                if (inCurrentDriveLabel) {
                    inCurrentDriveLabel.style.display = 'block';
                    inCurrentDriveLabel.disabled = false;

                    isCurrentDrive.addEventListener('change', function() {
                        if (isCurrentDrive.checked) {
                            driveTypeDiv.style.display = 'none';
                            driveTypeDiv.style.disabled = false;
                        } else {
                            driveTypeDiv.style.display = 'block';
                            driveTypeDiv.style.disabled = true;
                        }
                    });
                }

                folderTypeDiv.style.display = 'block';
                dateTypeDiv.style.display = 'block';
            }
        }
    }
});