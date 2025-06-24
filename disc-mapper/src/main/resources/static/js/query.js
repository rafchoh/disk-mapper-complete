$(document).ready(function() {
    $('#loadMoreBtn').on('click', function() {
        let container = $('#results-container');
        let loadMoreDiv = $(this).parent();

        let offset = parseInt(loadMoreDiv.attr('data-current-loaded')) || 0;
        let size = parseInt(loadMoreDiv.attr('data-size')) || 100;

        $('#loadingSpinner').show();

        $.ajax({
            url: '/search/load-more',
            method: 'POST',
            data: {
                offset: offset,
                size: size
            },
            success: function(fragmentHtml) {
                container.append(fragmentHtml);
                $('#loadingSpinner').hide();

                offset += size;
                loadMoreDiv.attr('data-current-loaded', offset);

                $('#statusText').text(offset);

                let totalResults = parseInt(loadMoreDiv.attr('data-total-results'));
                if (offset >= totalResults) {
                    loadMoreDiv.hide();
                }
            },
            error: function() {
                $('#loadingSpinner').hide();
                alert('Error while loading!');
            }
        });
    });
});





    $(document).ready(function() {
        let typingTimer;
        let debounceInterval = 400;

        $('#username').on('keyup', function() {
            clearTimeout(typingTimer);

            let username = $(this).val().trim();

            if (username.length === 0) {
                $('#username-status').text('');
                return;
            }

            typingTimer = setTimeout(function() {
                $.ajax({
                    url: '/user/login/checkUsername',
                    type: 'GET',
                    data: { username: username },
                    success: function(response) {
                        if (response.exists) {
                            $('#username-status')
                                .css('color', 'red')
                                .text('Taken');
                        } else {
                            $('#username-status')
                                .css('color', 'green')
                                .text('Available');
                        }
                    },
                    error: function() {
                        alert('Error while checking!');
                    }
                });
            }, debounceInterval);
        });
    });





    $(document).ready(function () {
        let typingTimer = {};
        let originalValues = {};
        let debounceInterval = 400;

        function attachDebouncedCheck(inputSelector, checkUrl) {
            let $input = $(inputSelector);
            let inputId = $input.attr('id');

            if (!$input.length) {
                return;
            }

            originalValues[inputId] = $input.val();

            $(inputSelector).on('keyup', function () {
                let value = $input.val();

                clearTimeout(typingTimer[inputId]);

                if (value.length === 0) {
                    $input.css('border', '');
                    return;
                }

                if (value === originalValues[inputId]) {
                    $input.css('border', '');
                    return;
                }

                typingTimer[inputId] = setTimeout(function () {
                    $.ajax({
                        url: checkUrl,
                        type: 'GET',
                        data: { name: value },
                        success: function (response) {
                            if (response.exists) {
                                $input.css('border', '3px solid red');
                            } else {
                                $input.css('border', '3px solid green');
                            }
                        },
                        error: function () {
                            $input.css('border', '3px solid orange');
                        }
                    });
                }, debounceInterval);
            });
        }

        attachDebouncedCheck('#driveName', '/drive/checkDriveName');
        attachDebouncedCheck('#pcNameField', '/device/checkPcName');
    });