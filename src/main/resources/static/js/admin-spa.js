(function () {
    const ADMIN_PREFIX = '/admin';
    const contentSelector = '[data-admin-content]';
    const loadedScripts = window.__adminLoadedScripts || new Set();
    window.__adminLoadedScripts = loadedScripts;
    document.querySelectorAll('script[src]').forEach(script => loadedScripts.add(script.src));

    function contentEl() {
        return document.querySelector(contentSelector);
    }

    function toUrl(value) {
        try {
            return new URL(value, window.location.origin);
        } catch (e) {
            return null;
        }
    }

    function isAdminGetUrl(url) {
        return url
            && url.origin === window.location.origin
            && (url.pathname === ADMIN_PREFIX || url.pathname.startsWith(ADMIN_PREFIX + '/'));
    }

    function shouldSkipLink(link, event) {
        if (!link || event.defaultPrevented) return true;
        if (event.button !== 0) return true;
        if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return true;
        if (link.target && link.target !== '_self') return true;
        if (link.hasAttribute('download')) return true;
        if (link.closest('[data-no-spa]')) return true;
        if (link.getAttribute('href') === '#') return true;
        return false;
    }

    function setLoading(isLoading) {
        const main = contentEl();
        if (!main) return;
        main.classList.toggle('admin-content-loading', isLoading);
        main.setAttribute('aria-busy', isLoading ? 'true' : 'false');
    }

    function closeMobileSidebar() {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('sidebarOverlay');
        if (sidebar) sidebar.classList.remove('open');
        if (overlay) overlay.classList.add('hidden');
    }

    function normalizePath(path) {
        if (!path) return ADMIN_PREFIX;
        return path.length > 1 ? path.replace(/\/+$/, '') : path;
    }

    function updateActiveSidebar(url) {
        const path = normalizePath(url.pathname);
        document.querySelectorAll('.sidebar .nav-item[href]').forEach(link => {
            const linkUrl = toUrl(link.getAttribute('href'));
            if (!isAdminGetUrl(linkUrl)) return;

            const linkPath = normalizePath(linkUrl.pathname);
            let active = false;
            if (linkPath === ADMIN_PREFIX) {
                active = path === ADMIN_PREFIX;
            } else {
                active = path === linkPath || path.startsWith(linkPath + '/');
            }
            link.classList.toggle('active', active);
        });
    }

    function updateTopbar(doc) {
        const nextTitle = doc.querySelector('title');
        if (nextTitle) document.title = nextTitle.textContent;

        const nextBreadcrumb = doc.getElementById('adminBreadcrumbTitle');
        const breadcrumb = document.getElementById('adminBreadcrumbTitle');
        if (nextBreadcrumb && breadcrumb) {
            breadcrumb.textContent = nextBreadcrumb.textContent;
        }
    }

    function copyAttributes(from, to) {
        Array.from(from.attributes).forEach(attr => {
            to.setAttribute(attr.name, attr.value);
        });
    }

    function loadExternalScript(oldScript) {
        const src = oldScript.src;
        if (!src || loadedScripts.has(src)) {
            return Promise.resolve();
        }

        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            copyAttributes(oldScript, script);
            script.onload = function () {
                loadedScripts.add(src);
                resolve();
            };
            script.onerror = reject;
            document.body.appendChild(script);
        });
    }

    async function executeScripts(container) {
        const scripts = Array.from(container.querySelectorAll('script'));
        for (const oldScript of scripts) {
            if (oldScript.src) {
                await loadExternalScript(oldScript);
                oldScript.remove();
                continue;
            }

            const script = document.createElement('script');
            copyAttributes(oldScript, script);
            script.textContent = oldScript.textContent;
            oldScript.replaceWith(script);
        }
    }

    async function navigate(url, options) {
        const targetUrl = toUrl(url);
        if (!isAdminGetUrl(targetUrl)) {
            window.location.href = url;
            return;
        }

        const main = contentEl();
        if (!main) {
            window.location.href = targetUrl.href;
            return;
        }

        setLoading(true);
        try {
            if (typeof window.__adminPageCleanup === 'function') {
                window.__adminPageCleanup();
                window.__adminPageCleanup = null;
            }

            const response = await fetch(targetUrl.href, {
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Accept': 'text/html'
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error('Navigation failed: ' + response.status);
            }

            const html = await response.text();
            const doc = new DOMParser().parseFromString(html, 'text/html');
            const nextMain = doc.querySelector(contentSelector);

            if (!nextMain) {
                window.location.href = targetUrl.href;
                return;
            }

            main.innerHTML = nextMain.innerHTML;
            updateTopbar(doc);
            updateActiveSidebar(targetUrl);
            closeMobileSidebar();

            if (!options || options.push !== false) {
                history.pushState({ adminSpa: true }, '', targetUrl.href);
            }

            await executeScripts(main);
            main.dispatchEvent(new CustomEvent('admin:content-loaded', {
                bubbles: true,
                detail: { url: targetUrl.href }
            }));
            main.scrollTo({ top: 0, left: 0 });
            window.scrollTo(0, 0);
        } catch (error) {
            console.warn(error);
            window.location.href = targetUrl.href;
        } finally {
            setLoading(false);
        }
    }

    document.addEventListener('click', function (event) {
        const link = event.target.closest('a[href]');
        if (shouldSkipLink(link, event)) return;

        const url = toUrl(link.getAttribute('href'));
        if (!isAdminGetUrl(url)) return;

        event.preventDefault();
        navigate(url.href);
    });

    document.addEventListener('submit', function (event) {
        if (event.defaultPrevented) return;

        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;
        if (form.closest('[data-no-spa]')) return;

        const method = (form.getAttribute('method') || 'get').toLowerCase();
        if (method !== 'get') return;
        if (form.enctype && form.enctype !== 'application/x-www-form-urlencoded') return;

        const actionUrl = toUrl(form.getAttribute('action') || window.location.href);
        if (!isAdminGetUrl(actionUrl)) return;

        const params = new URLSearchParams(new FormData(form));
        actionUrl.search = params.toString();

        event.preventDefault();
        navigate(actionUrl.href);
    });

    window.addEventListener('popstate', function () {
        const url = toUrl(window.location.href);
        if (isAdminGetUrl(url)) {
            navigate(url.href, { push: false });
        }
    });

    updateActiveSidebar(new URL(window.location.href));
})();
