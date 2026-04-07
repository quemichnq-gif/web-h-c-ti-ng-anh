document.addEventListener("DOMContentLoaded", function () {
    const alerts = Array.from(document.querySelectorAll("[data-auto-dismiss]"));

    alerts.forEach(function (alertEl) {
        const delay = Number.parseInt(alertEl.getAttribute("data-auto-dismiss"), 10);

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
