window.com_github_wolfie_history_PushStateLink = function() {
    
    var element = this.getElement();
    var self = this;
    var a = document.createElement("a");

    a.addEventListener("click", function(e) {
        window.history.pushState('object', a.innerText, a.href);
        e.preventDefault();
        self.onClick();
    });

    this.onStateChange = function() {
        a.innerText = this.getState().text;
        a.href = this.getState().href;
        element.appendChild(a);
    };
	
};