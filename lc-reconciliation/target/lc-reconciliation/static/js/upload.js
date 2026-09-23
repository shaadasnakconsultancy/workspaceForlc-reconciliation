// LC Upload Wizard JavaScript
var batchId = null;
var masterLcFiles = [];
var addendumFiles = [];
var supportingDocs = [];
var supportingCounter = 0;

$(document).ready(function() {
    var contextPath = $('link[href*="app.css"]').attr('href').replace('/static/css/app.css', '');

    // Drag and drop for Master LC
    setupDropZone('#masterLcZone', '#masterLcFile', false, function(files) {
        masterLcFiles = [files[0]];
        renderFileList('#masterLcList', masterLcFiles, function(idx) { masterLcFiles = []; renderFileList('#masterLcList', masterLcFiles); });
    });

    // Drag and drop for Addendum
    setupDropZone('#addendumZone', '#addendumFiles', true, function(files) {
        for (var i = 0; i < files.length; i++) {
            addendumFiles.push(files[i]);
        }
        renderFileList('#addendumList', addendumFiles, function(idx) { addendumFiles.splice(idx, 1); renderFileList('#addendumList', addendumFiles); });
    });

    // Document type selection shows page limit
    $('#docTypeSelect').on('change', function() {
        var pages = $(this).find(':selected').data('pages');
        $('#pageLimitInfo').text(pages || '');
    });

    // Add supporting document
    $('#btnAddSupporting').on('click', function() {
        var docTypeId = $('#docTypeSelect').val();
        var docTypeName = $('#docTypeSelect option:selected').text();
        var pages = $('#docTypeSelect option:selected').data('pages');
        var fileInput = document.getElementById('supportingFile');

        if (!docTypeId) { showToast('Please select a document type', 'error'); return; }
        if (!fileInput.files.length) { showToast('Please select a file', 'error'); return; }

        var file = fileInput.files[0];
        var docTypeCode = $('#docTypeSelect option:selected').data('code');
        supportingCounter++;
        supportingDocs.push({
            id: supportingCounter,
            docTypeId: docTypeId,
            docTypeCode: docTypeCode,
            docTypeName: docTypeName,
            pages: pages,
            file: file
        });

        renderSupportingTable();
        $('#docTypeSelect').val('');
        $('#supportingFile').val('');
        $('#pageLimitInfo').text('');
    });

    // Step navigation
    $('#btnNextStep2').on('click', function() {
        if (masterLcFiles.length === 0) { showToast('Please upload Master LC document', 'error'); return; }
        goToStep(2);
    });
    $('#btnBackStep1').on('click', function() { goToStep(1); });
    $('#btnNextStep3').on('click', function() {
        if (supportingDocs.length === 0) { showToast('Please add at least one supporting document', 'error'); return; }
        // Check: if any non-Invoice doc is uploaded, Invoice must also be present
        var hasInvoice = false;
        var hasOtherDocs = false;
        for (var i = 0; i < supportingDocs.length; i++) {
            var code = supportingDocs[i].docTypeCode || '';
            if (code === 'INVOICE') hasInvoice = true;
            else hasOtherDocs = true;
        }
        if (hasOtherDocs && !hasInvoice) {
            showToast('Invoice document is mandatory when uploading other supporting documents. Please add Invoice first.', 'error');
            return;
        }
        populateReview();
        goToStep(3);
    });
    $('#btnBackStep2').on('click', function() { goToStep(2); });

    // Load email groups
    $.ajax({
        url: contextPath + '/master/email-groups?format=json',
        type: 'GET',
        dataType: 'json',
        success: function(groups) {
            if (groups && groups.length) {
                for (var i = 0; i < groups.length; i++) {
                    $('#emailGroupSelect').append('<option value="' + groups[i].id + '">' + groups[i].groupName + '</option>');
                }
            }
        }
    });

    // Start Reconciliation
    $('#btnStartRecon').on('click', function() {
        startReconciliation();
    });
});

function setupDropZone(zoneSelector, inputSelector, multiple, onFiles) {
    var zone = $(zoneSelector);
    var input = $(inputSelector);

    zone.on('click', function() { input.click(); });
    input.on('change', function() { if (this.files.length) onFiles(this.files); });

    zone.on('dragover', function(e) { e.preventDefault(); zone.addClass('dragover'); });
    zone.on('dragleave', function() { zone.removeClass('dragover'); });
    zone.on('drop', function(e) {
        e.preventDefault();
        zone.removeClass('dragover');
        if (e.originalEvent.dataTransfer.files.length) onFiles(e.originalEvent.dataTransfer.files);
    });
}

function renderFileList(containerSelector, files, onRemove) {
    var html = '';
    for (var i = 0; i < files.length; i++) {
        html += '<div class="d-flex align-items-center justify-content-between border rounded p-2 mb-1">';
        html += '<span><i class="bi bi-file-earmark text-primary"></i> ' + files[i].name + ' <small class="text-muted">(' + formatFileSize(files[i].size) + ')</small></span>';
        html += '<button class="btn btn-sm btn-outline-danger" data-idx="' + i + '"><i class="bi bi-x"></i></button>';
        html += '</div>';
    }
    $(containerSelector).html(html);
    $(containerSelector).find('button').on('click', function() { onRemove($(this).data('idx')); });
}

function renderSupportingTable() {
    var tbody = $('#supportingDocsTable tbody');
    tbody.empty();
    for (var i = 0; i < supportingDocs.length; i++) {
        var d = supportingDocs[i];
        tbody.append('<tr>' +
            '<td>' + (i+1) + '</td>' +
            '<td>' + d.docTypeName + '</td>' +
            '<td>' + d.file.name + '</td>' +
            '<td>' + formatFileSize(d.file.size) + '</td>' +
            '<td>' + d.pages + '</td>' +
            '<td><button class="btn btn-sm btn-outline-danger btn-remove-supporting" data-idx="' + i + '"><i class="bi bi-trash"></i></button></td>' +
            '</tr>');
    }
    tbody.find('.btn-remove-supporting').on('click', function() {
        supportingDocs.splice($(this).data('idx'), 1);
        renderSupportingTable();
    });
}

function goToStep(step) {
    $('.step-content').hide();
    $('#step' + step).show();
    for (var i = 1; i <= 3; i++) {
        var indicator = $('#stepIndicator' + i);
        indicator.removeClass('active completed');
        if (i < step) indicator.addClass('completed');
        if (i === step) indicator.addClass('active');
    }
}

function populateReview() {
    var lcHtml = '';
    for (var i = 0; i < masterLcFiles.length; i++) {
        var url = URL.createObjectURL(masterLcFiles[i]);
        lcHtml += '<div class="d-flex align-items-center justify-content-between border rounded p-2 mb-1">';
        lcHtml += '<span><i class="bi bi-file-earmark text-primary"></i> ' + masterLcFiles[i].name + ' <span class="badge bg-primary">Master LC</span></span>';
        lcHtml += '<a href="' + url + '" target="_blank" class="btn btn-sm btn-outline-secondary"><i class="bi bi-eye"></i> View</a>';
        lcHtml += '</div>';
    }
    for (var i = 0; i < addendumFiles.length; i++) {
        var url = URL.createObjectURL(addendumFiles[i]);
        lcHtml += '<div class="d-flex align-items-center justify-content-between border rounded p-2 mb-1">';
        lcHtml += '<span><i class="bi bi-file-earmark text-primary"></i> ' + addendumFiles[i].name + ' <span class="badge bg-secondary">Amendment</span></span>';
        lcHtml += '<a href="' + url + '" target="_blank" class="btn btn-sm btn-outline-secondary"><i class="bi bi-eye"></i> View</a>';
        lcHtml += '</div>';
    }
    $('#reviewLcDocs').html(lcHtml);

    var supHtml = '<table class="table table-sm table-bordered"><thead><tr><th>Type</th><th>File</th><th>Pages</th><th>Action</th></tr></thead><tbody>';
    for (var i = 0; i < supportingDocs.length; i++) {
        var url = URL.createObjectURL(supportingDocs[i].file);
        supHtml += '<tr><td>' + supportingDocs[i].docTypeName + '</td><td>' + supportingDocs[i].file.name + '</td><td>' + supportingDocs[i].pages + '</td>';
        supHtml += '<td><a href="' + url + '" target="_blank" class="btn btn-sm btn-outline-secondary"><i class="bi bi-eye"></i> View</a></td></tr>';
    }
    supHtml += '</tbody></table>';
    $('#reviewSupportingDocs').html(supHtml);
}

function startReconciliation() {
    var contextPath = $('link[href*="app.css"]').attr('href').replace('/static/css/app.css', '');
    $('.step-content').hide();
    $('#processingOverlay').show();
    $('#processingMessage').text('Uploading LC documents...');
    $('#uploadProgress').css('width', '10%');

    // Step 1: Upload LC documents
    var lcFormData = new FormData();
    lcFormData.append('action', 'uploadLC');
    for (var i = 0; i < masterLcFiles.length; i++) {
        lcFormData.append('masterLc', masterLcFiles[i]);
    }
    for (var i = 0; i < addendumFiles.length; i++) {
        lcFormData.append('addendum', addendumFiles[i]);
    }

    $.ajax({
        url: contextPath + '/lc-upload',
        type: 'POST',
        data: lcFormData,
        processData: false,
        contentType: false,
        success: function(res) {
            if (!res.success) { showToast(res.message, 'error'); goToStep(1); return; }
            batchId = res.batchId;
            $('#uploadProgress').css('width', '40%');
            $('#processingMessage').text('Uploading supporting documents...');
            uploadSupportingDocs();
        },
        error: function() { showToast('Failed to upload LC documents', 'error'); goToStep(1); }
    });
}

function uploadSupportingDocs() {
    var contextPath = $('link[href*="app.css"]').attr('href').replace('/static/css/app.css', '');
    var formData = new FormData();
    formData.append('action', 'uploadSupporting');
    formData.append('batchId', batchId);

    for (var i = 0; i < supportingDocs.length; i++) {
        var d = supportingDocs[i];
        formData.append('file_' + i, d.file);
        formData.append('docTypeId_' + i, d.docTypeId);
        formData.append('documentTypeId', d.docTypeId);
    }

    $.ajax({
        url: contextPath + '/lc-upload',
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function(res) {
            if (!res.success) { showToast(res.message, 'error'); return; }
            $('#uploadProgress').css('width', '70%');
            $('#processingMessage').text('Starting reconciliation job...');
            startJob();
        },
        error: function() { showToast('Failed to upload supporting documents', 'error'); }
    });
}

function startJob() {
    var contextPath = $('link[href*="app.css"]').attr('href').replace('/static/css/app.css', '');
    var formData = new FormData();
    formData.append('action', 'startJob');
    formData.append('batchId', batchId);
    formData.append('notificationEmail', $('#notificationEmail').val());
    formData.append('jobName', $('#jobName').val());
    formData.append('shipmentDocName', $('#shipmentDocName').val());
    formData.append('emailGroupId', $('#emailGroupSelect').val() || '0');

    $.ajax({
        url: contextPath + '/lc-upload',
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function(res) {
            if (!res.success) { showToast(res.message, 'error'); return; }
            $('#uploadProgress').css('width', '100%');
            $('#processingMessage').text('Job started! Redirecting to Job Monitor...');
            setTimeout(function() {
                window.location.href = contextPath + '/job-monitor?jobId=' + res.jobId;
            }, 1500);
        },
        error: function() { showToast('Failed to start job', 'error'); }
    });
}
