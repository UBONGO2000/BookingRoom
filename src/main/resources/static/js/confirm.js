// Confirmation des actions destructrices sans attribut inline (compatible CSP).
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-confirm]').forEach((element) => {
        const message = element.getAttribute('data-confirm');
        if (!message) {
            return;
        }

        if (element.tagName === 'FORM') {
            element.addEventListener('submit', (event) => {
                if (!window.confirm(message)) {
                    event.preventDefault();
                }
            });
            return;
        }

        element.addEventListener('click', (event) => {
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });
});
