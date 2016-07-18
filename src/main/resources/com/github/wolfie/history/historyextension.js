window.com_github_wolfie_history_HistoryExtension = function () {
    var connector = this;

    // feature detection
    if (!window.history.pushState || !window.history.replaceState) {
        // error code 0 = unsupported feature
        connector.error(0, "Unsupported Browser",
                "history.pushState and/or history.replaceState not supported "
                + "by the user's browser: "
                + window.navigator.userAgent);
    }

    // proxied functions
    this.back = function () {
        window.history.back();
    };
    this.forward = function () {
        window.history.forward();
    };
    this.go = function (steps) {
        window.history.go(steps);
    };

    // wrapped functions
    this.pushState = function (state, url) {
        try {
            // Do not pass pushState if already there
            if (window.location.href != window.location.protocol + "//" + window.location.host + url) {
                // url might be undefined or null, but that's okay.
                try {
                    state = JSON.parse(state);
                } catch(e){}
                window.history.pushState(state, "", url);
            }
        } catch (e) {
            // error code 1 = error on method invoke
            connector.error(1, e.name, e.message, window.location.href);
        }
    };

    this.replaceState = function (state, url) {
        try {
            // url might be undefined or null, but that's okay.
                try {
                    state = JSON.parse(state);
                } catch(e){}
            window.history.replaceState(state, "", url);
        } catch (e) {
            // error code 1 = error on method invoke
            connector.error(1, e.name, e.message, window.location.href);
        }
    };

    if (window['addEventListener'] !== undefined) {
        window.addEventListener("popstate", function (e) {
            connector.popstate(e.state, window.location.href);
        });
    }
};