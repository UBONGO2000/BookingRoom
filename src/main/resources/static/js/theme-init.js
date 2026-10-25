// Applique le theme stocke avant le premier rendu pour eviter tout flash.
// Ce script est charge de maniere synchrone dans le <head> et reste externe
// pour respecter la CSP (aucun script inline).
(function () {
    try {
        var stored = window.localStorage.getItem('theme');
        var theme = stored === 'dark' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', theme);
    } catch (error) {
        document.documentElement.setAttribute('data-theme', 'light');
    }
})();
