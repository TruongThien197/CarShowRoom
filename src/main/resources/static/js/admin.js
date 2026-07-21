/**
 * admin.js — behaviours specific to the /admin section.
 * Loaded only on admin pages (dashboard + management lists), in addition
 * to the shared app.js. Depends on Chart.js being loaded first when
 * initOrdersChart() is used.
 */
(function () {
    "use strict";

    /**
     * Renders the "orders per month" bar chart on the admin dashboard.
     * Called from admin/dashboard.html with server-rendered data, so the
     * page's inline <script> stays a one-line data hand-off instead of
     * duplicating Chart.js config markup.
     */
    window.initOrdersChart = function (canvasId, labels, counts) {
        var canvas = document.getElementById(canvasId);
        if (!canvas || typeof Chart === "undefined") {
            return null;
        }
        return new Chart(canvas, {
            type: "bar",
            data: {
                labels: labels,
                datasets: [{
                    label: "Order count",
                    data: counts,
                    backgroundColor: "rgba(63, 160, 78, 0.9)",
                    borderColor: "rgba(63, 160, 78, 1)",
                    borderWidth: 1,
                    borderRadius: 5,
                    maxBarThickness: 46
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {beginAtZero: true, ticks: {precision: 0}}
                },
                plugins: {
                    legend: {display: false},
                    tooltip: {callbacks: {label: function (context) { return context.parsed.y + " orders"; }}}
                }
            }
        });
    };

    /**
     * Client-side search filter for admin tables.
     * Usage: <input data-table-filter="#myTable"> — filters rows of the
     * given <table> by matching typed text against each row's text content.
     * Pure UX enhancement, no backend/pagination changes required.
     */
    function initTableFilters() {
        document.querySelectorAll("[data-table-filter]").forEach(function (input) {
            var table = document.querySelector(input.getAttribute("data-table-filter"));
            if (!table) {
                return;
            }
            var tbody = table.tBodies[0];
            if (!tbody) {
                return;
            }
            var rows = Array.prototype.slice.call(tbody.rows);
            input.addEventListener("input", function () {
                var term = input.value.trim().toLowerCase();
                var visibleCount = 0;
                rows.forEach(function (row) {
                    var matches = row.textContent.toLowerCase().indexOf(term) !== -1;
                    row.style.display = matches ? "" : "none";
                    if (matches) {
                        visibleCount += 1;
                    }
                });
                var emptyHint = table.parentElement.querySelector(".table-filter-empty");
                if (emptyHint) {
                    emptyHint.classList.toggle("d-none", visibleCount !== 0 || term === "");
                }
            });
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        initTableFilters();
    });
})();
