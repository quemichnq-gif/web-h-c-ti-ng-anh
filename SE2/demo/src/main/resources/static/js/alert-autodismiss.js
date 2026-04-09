document.addEventListener("DOMContentLoaded", function () {
    const alerts = Array.from(document.querySelectorAll("[data-auto-dismiss], .alert-success"));

    alerts.forEach(function (alertEl) {
        const delayAttr = alertEl.getAttribute("data-auto-dismiss");
        const delay = Number.parseInt(delayAttr || "1500", 10);

        if (!Number.isFinite(delay) || delay < 0) {
            return;
        }

        window.setTimeout(function () {
            alertEl.classList.add("is-fading");
            window.setTimeout(function () {
                alertEl.remove();
            }, 260);
        }, delay);
    });
});
