window.com_github_wolfie_history_HistoryExtension = function() {
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
	this.back = function() { window.history.back(); };
	this.forward = function() { window.history.forward(); };
	this.go = function(steps) { window.history.go(steps); };
	
	// wrapped functions
	this.pushState = function(state, url) {
		try {
			window.history.pushState(state, "", url);
		} catch (e) {
			// error code 1 = error on method invoke
			connector.error(1, e.name, e.message);
		}
	};
	
	this.replaceState = function(state, url) {
		try {
			window.history.replaceState(state, "", url);
		} catch (e) {
			// error code 1 = error on method invoke
			connector.error(1, e.name, e.message);
		}
	};
	
	if (window['addEventListener'] !== undefined) {
		window.addEventListener("popstate", function(e) {
			connector.popstate(e.state);
		});
	}
};