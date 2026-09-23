<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>LC Upload - LC Reconciliation</title>
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
</head>
<body>
    <c:set var="pageTitle" value="LC Upload" scope="request" />
    <%@ include file="layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="layout/header.jsp" %>
        <div class="container-fluid p-4">
            <!-- Step Wizard -->
            <div class="step-wizard">
                <div class="step active" id="stepIndicator1">
                    <span class="step-number">1</span>
                    <span class="step-label">LC Documents</span>
                </div>
                <div class="step-line"></div>
                <div class="step" id="stepIndicator2">
                    <span class="step-number">2</span>
                    <span class="step-label">Supporting Documents</span>
                </div>
                <div class="step-line"></div>
                <div class="step" id="stepIndicator3">
                    <span class="step-number">3</span>
                    <span class="step-label">Review & Submit</span>
                </div>
            </div>

            <!-- Step 1: Upload LC Documents -->
            <div id="step1" class="step-content">
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-file-earmark-pdf"></i> Master LC Document</h5></div>
                    <div class="card-body">
                        <div class="upload-zone" id="masterLcZone">
                            <i class="bi bi-cloud-arrow-up"></i>
                            <p class="mt-2 mb-0">Drag & Drop Master LC Document (PDF, Word, Excel) or click to browse</p>
                            <input type="file" id="masterLcFile" accept=".pdf,.docx,.doc,.xlsx,.xlsm,.xls,.msg,.pptx,.ppt" style="display:none">
                        </div>
                        <div id="masterLcList" class="mt-3"></div>
                    </div>
                </div>
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-files"></i> Amendment Documents (Optional)</h5></div>
                    <div class="card-body">
                        <div class="upload-zone" id="addendumZone">
                            <i class="bi bi-cloud-arrow-up"></i>
                            <p class="mt-2 mb-0">Drag & Drop Amendment Documents (PDF, Word, Excel) - Multiple allowed</p>
                            <input type="file" id="addendumFiles" accept=".pdf,.docx,.doc,.xlsx,.xlsm,.xls,.msg,.pptx,.ppt" multiple style="display:none">
                        </div>
                        <div id="addendumList" class="mt-3"></div>
                    </div>
                </div>
                <div class="text-end">
                    <button class="btn btn-primary" id="btnNextStep2">Next: Upload Supporting Documents <i class="bi bi-arrow-right"></i></button>
                </div>
            </div>

            <!-- Step 2: Upload Supporting Documents -->
            <div id="step2" class="step-content" style="display:none">
                <div class="card mb-4">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><i class="bi bi-file-earmark-plus"></i> Upload Supporting Documents</h5>
                    </div>
                    <div class="card-body">
                        <div class="row mb-3" id="supportingUploadRow">
                            <div class="col-md-4">
                                <select class="form-select" id="docTypeSelect">
                                    <option value="">-- Select Document Type --</option>
                                    <c:forEach var="dt" items="${documentTypes}">
                                        <option value="${dt.id}" data-code="<c:out value='${dt.typeCode}'/>" data-pages="${dt.pageLimitDisplay}"><c:out value="${dt.typeName}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <input type="file" class="form-control" id="supportingFile" accept=".pdf,.docx,.doc,.xlsx,.xlsm,.xls,.msg,.pptx,.ppt">
                            </div>
                            <div class="col-md-2">
                                <span class="form-text" id="pageLimitInfo"></span>
                            </div>
                            <div class="col-md-2">
                                <button class="btn btn-success" id="btnAddSupporting"><i class="bi bi-plus-circle"></i> Add</button>
                            </div>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-bordered" id="supportingDocsTable">
                                <thead class="table-light">
                                    <tr>
                                        <th>#</th>
                                        <th>Document Type</th>
                                        <th>File Name</th>
                                        <th>Size</th>
                                        <th>Pages</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody></tbody>
                            </table>
                        </div>
                    </div>
                </div>
                <div class="d-flex justify-content-between">
                    <button class="btn btn-secondary" id="btnBackStep1"><i class="bi bi-arrow-left"></i> Back</button>
                    <button class="btn btn-primary" id="btnNextStep3">Next: Review & Submit <i class="bi bi-arrow-right"></i></button>
                </div>
            </div>

            <!-- Step 3: Review & Submit -->
            <div id="step3" class="step-content" style="display:none">
                <div class="card mb-4">
                    <div class="card-header"><h5 class="mb-0"><i class="bi bi-check2-square"></i> Review & Start Reconciliation</h5></div>
                    <div class="card-body">
                        <h6>LC Documents:</h6>
                        <div id="reviewLcDocs" class="mb-3"></div>
                        <h6>Supporting Documents:</h6>
                        <div id="reviewSupportingDocs" class="mb-3"></div>
                        <hr>
                        <div class="row mb-3">
                            <div class="col-md-4">
                                <label class="form-label">Job Name (Optional)</label>
                                <input type="text" class="form-control" id="jobName" placeholder="e.g. BNG855 Shipment 1">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Shipment Document Name</label>
                                <input type="text" class="form-control" id="shipmentDocName" placeholder="e.g. March 2026 Shipment">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label">Email Group</label>
                                <select class="form-select" id="emailGroupSelect">
                                    <option value="">-- No Group (only you) --</option>
                                </select>
                                <div class="form-text">You always receive the report; a group adds its members.</div>
                            </div>
                        </div>
                        <%-- No implicit recipient: leaving the group unselected means no notification is
                             sent. This previously defaulted to the submitting user's address, so every
                             job silently mailed whoever started it. --%>
                        <input type="hidden" id="notificationEmail" value="">
                    </div>
                </div>
                <div class="d-flex justify-content-between">
                    <button class="btn btn-secondary" id="btnBackStep2"><i class="bi bi-arrow-left"></i> Back</button>
                    <button class="btn btn-success btn-lg" id="btnStartRecon"><i class="bi bi-play-circle"></i> Start Reconciliation</button>
                </div>
            </div>

            <!-- Processing Overlay -->
            <div id="processingOverlay" style="display:none" class="text-center py-5">
                <div class="spinner-border text-primary mb-3" role="status" style="width:3rem;height:3rem"></div>
                <h5 id="processingMessage">Uploading files...</h5>
                <div class="progress mx-auto" style="max-width:400px;height:25px">
                    <div class="progress-bar progress-bar-striped progress-bar-animated" id="uploadProgress" style="width:0%"></div>
                </div>
            </div>
        </div>
        <%@ include file="layout/footer.jsp" %>
    </div>
    <script src="${pageContext.request.contextPath}/static/vendor/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/vendor/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/upload.js?v=3"></script>
</body>
</html>
