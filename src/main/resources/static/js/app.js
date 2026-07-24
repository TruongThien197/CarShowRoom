/**
 * app.js — GearShift Pro shared front-end behaviours.
 * Loaded globally through fragments/footer.html :: scripts on every page
 * (customer + admin). Keeps small, dependency-free enhancements that any
 * template can opt into via data-* attributes, without requiring backend
 * changes from other members' controllers.
 */
(function () {
    "use strict";

    /**
     * 1) Confirm-before-submit.
     * Any form or link with data-confirm displays a Bootstrap modal instead
     * of the browser's native confirmation dialog.
     */
    function initConfirmActions() {
        if (typeof bootstrap === "undefined" || !bootstrap.Modal) return;

        var modalElement = document.createElement("div");
        modalElement.className = "modal fade";
        modalElement.id = "confirmActionModal";
        modalElement.tabIndex = -1;
        modalElement.setAttribute("aria-labelledby", "confirmActionModalLabel");
        modalElement.setAttribute("aria-hidden", "true");
        modalElement.innerHTML =
            '<div class="modal-dialog modal-dialog-centered">' +
                '<div class="modal-content">' +
                    '<div class="modal-header">' +
                        '<h5 class="modal-title" id="confirmActionModalLabel">Xác nhận thao tác</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>' +
                    '</div>' +
                    '<div class="modal-body" id="confirmActionMessage"></div>' +
                    '<div class="modal-footer">' +
                        '<button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button>' +
                        '<button type="button" class="btn btn-danger" id="confirmActionButton">Xác nhận</button>' +
                    '</div>' +
                '</div>' +
            '</div>';
        document.body.appendChild(modalElement);

        var modal = new bootstrap.Modal(modalElement);
        var messageElement = modalElement.querySelector("#confirmActionMessage");
        var confirmButton = modalElement.querySelector("#confirmActionButton");
        var pendingAction = null;

        function showConfirmation(message, action) {
            messageElement.textContent = message || "Bạn có chắc muốn thực hiện thao tác này?";
            pendingAction = action;
            modal.show();
        }

        confirmButton.addEventListener("click", function () {
            var action = pendingAction;
            pendingAction = null;
            modal.hide();
            if (action) action();
        });

        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (form.dataset.confirmed === "true") {
                    delete form.dataset.confirmed;
                    return;
                }
                event.preventDefault();
                showConfirmation(form.getAttribute("data-confirm"), function () {
                    form.dataset.confirmed = "true";
                    form.requestSubmit();
                });
            });
        });

        document.querySelectorAll("a[data-confirm]").forEach(function (link) {
            link.addEventListener("click", function (event) {
                event.preventDefault();
                showConfirmation(link.getAttribute("data-confirm"), function () {
                    window.location.assign(link.href);
                });
            });
        });
    }

    /**
     * 2) Client-side Bootstrap form validation.
     * Any <form class="needs-validation"> gets HTML5 + Bootstrap validation
     * styling on submit, blocking submission until required fields are valid.
     */
    function initFormValidation() {
        document.querySelectorAll("form.needs-validation").forEach(function (form) {
            form.setAttribute("novalidate", "novalidate");
            form.addEventListener("submit", function (event) {
                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add("was-validated");
            }, false);
        });
    }

    /**
     * 3) Flash message toasts.
     * fragments/footer.html renders a hidden template (#flashToastTemplate)
     * whenever the server sets successMessage / errorMessage. This wires it
     * up as an auto-dismissing Bootstrap toast instead of a static banner.
     */
    function initFlashToasts() {
        var toastEl = document.getElementById("flashToast");
        if (!toastEl || typeof bootstrap === "undefined" || !bootstrap.Toast) {
            return;
        }
        var toast = new bootstrap.Toast(toastEl, {delay: 4000});
        toast.show();
    }

    /**
     * 4) Auto-dismiss plain Bootstrap alert banners (fallback for pages that
     * still render a static .alert instead of the toast template) so old
     * success/error messages don't linger on the page forever.
     */
    function initAutoDismissAlerts() {
        document.querySelectorAll(".alert.alert-dismissible-auto").forEach(function (alertEl) {
            window.setTimeout(function () {
                if (typeof bootstrap !== "undefined" && bootstrap.Alert) {
                    bootstrap.Alert.getOrCreateInstance(alertEl).close();
                } else {
                    alertEl.remove();
                }
            }, 5000);
        });
    }

    /**
     * 5) Enable Bootstrap tooltips for any [data-bs-toggle="tooltip"] element.
     */
    function initTooltips() {
        if (typeof bootstrap === "undefined" || !bootstrap.Tooltip) {
            return;
        }
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (el) {
            bootstrap.Tooltip.getOrCreateInstance(el);
        });
    }

    /**
     * 6) Car-model checkbox selector count.
     * Any container with [data-car-model-count] is updated whenever a
     * checkbox inside the same .car-model-select-card changes.
     */
    function initCarModelSelector() {
        document.querySelectorAll("[data-car-model-count]").forEach(function (countEl) {
            var card = countEl.closest(".car-model-select-card");
            if (!card) return;
            var checkboxes = card.querySelectorAll(".car-model-chip-input");
            function updateCount() {
                var checked = card.querySelectorAll(".car-model-chip-input:checked").length;
                countEl.textContent = checked + " selected";
            }
            checkboxes.forEach(function (cb) {
                cb.addEventListener("change", updateCount);
            });
            updateCount();
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        initConfirmActions();
        initFormValidation();
        initFlashToasts();
        initAutoDismissAlerts();
        initTooltips();
        initCarModelSelector();
    });
})();
