document.addEventListener('DOMContentLoaded', () => {
    const themeToggle = document.getElementById('theme-toggle');
    if (!themeToggle) {
        return;
    }

    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    themeToggle.setAttribute('aria-pressed', currentTheme === 'dark' ? 'true' : 'false');

    themeToggle.addEventListener('click', () => {
        const theme = document.documentElement.getAttribute('data-theme');
        const newTheme = theme === 'light' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', newTheme);
        themeToggle.setAttribute('aria-pressed', newTheme === 'dark' ? 'true' : 'false');
        try {
            window.localStorage.setItem('theme', newTheme);
        } catch (error) {
            // Stockage indisponible (navigation privee) : le theme reste applique.
        }
    });
});
