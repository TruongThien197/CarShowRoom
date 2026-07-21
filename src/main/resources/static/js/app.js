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
     * Any <form> or <button>/<a> with data-confirm="message" shows a
     * confirmation dialog before the action proceeds. Replaces scattered
     * inline onclick="return confirm(...)" with one consistent mechanism.
     */
    function initConfirmActions() {
        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                var message = form.getAttribute("data-confirm") || "Are you sure you want to do this?";
                if (!window.confirm(message)) {
                    event.preventDefault();
                }
            });
        });

        document.querySelectorAll("a[data-confirm]").forEach(function (link) {
            link.addEventListener("click", function (event) {
                var message = link.getAttribute("data-confirm") || "Are you sure you want to do this?";
                if (!window.confirm(message)) {
                    event.preventDefault();
                }
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
