(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
var Api = {}

Api.requestWithFeedback = function(args, bind, fn, fnError) {
  
  var data = m.prop();
  var completed = m.prop(false);
  var complete = function() {
    completed(true)
  };
  args.background = true;
  args.config = function(xhr) {
    xhr.timeout = 4000;
    xhr.ontimeout = function() {
      complete();
      m.redraw();
    }
  };
  return {
    request: m.request(args).then(data).then(function(data){
      if(bind !== undefined) bind(data);
      if(fn !== undefined) fn();
      complete();
      m.redraw();
    }, function(error){
      if(fnError !== undefined)  fnError()
    }),
    data: data,
    ready: completed
  }
};

module.exports = Api;
},{}],2:[function(require,module,exports){
var Sidebar = function(ctrl){
  return [
    {tag: "div", attrs: {className:"ui sidebar inverted vertical visible menu"
    }, children: [
      {tag: "a", attrs: {className:"item", href:"#/"}, children: [
        "Home"
      ]}, 
      {tag: "a", attrs: {className:"item", href:"#/new/1"}, children: [
        "Type 1"
      ]}, 
      {tag: "a", attrs: {className:"item", href:"#/new/2"}, children: [
        "Type 2"
      ]}
    ]}
  ]
};

module.exports = Sidebar;
},{}],3:[function(require,module,exports){
var Api = require('../_api.msx');


var Controller = function(){
  var ctrl = this;
  
  ctrl.data = m.prop({});
  ctrl.setup = function(){
    ctrl.data().answers = ctrl.data().answers.split(',');
    m.redraw();
  }
  
  ctrl.request = Api.requestWithFeedback({method: "GET", url: "/api/admin/question/get/" + m.route.param('id')}, ctrl.data, ctrl.setup);
 
};


module.exports = Controller;
},{"../_api.msx":1}],4:[function(require,module,exports){
var Sidebar = require('../_sidebar.msx');
var Type1 = require('./type/1.msx');
var Type2 = require('./type/2.msx');
var list=[Type1, Type2];

var View = function(ctrl){
  return (
      {tag: "div", attrs: {}, children: [
        Sidebar(ctrl), 
        {tag: "div", attrs: {className:"pusher"}, children: [
          (!ctrl.request.ready())?"Loading":(
              list[ctrl.data().typeNum - 1](ctrl)
          )
        ]}
      ]}
  )
};

module.exports = View;
},{"../_sidebar.msx":2,"./type/1.msx":6,"./type/2.msx":7}],5:[function(require,module,exports){
var Controller = require('./_controller.msx');
var View = require('./_view.msx');

var Main = {
  controller: Controller,
  view: View
}

module.exports = Main;
},{"./_controller.msx":3,"./_view.msx":4}],6:[function(require,module,exports){


var View = function(ctrl){
  return [
    {tag: "div", attrs: {className:"ui  segment"}, children: [
      {tag: "h2", attrs: {}, children: ["(type ", ctrl.data().typeNum, ") Question ID : ", ctrl.data().id]}
    ]},
    {tag: "div", attrs: {className:"ui segment"}, children: [
      {tag: "div", attrs: {className:"ui form"}, children: [
        {tag: "h4", attrs: {class:"ui dividing header"}, children: ["Type 1"]}, 
        {tag: "div", attrs: {className:"field"}, children: [
          {tag: "label", attrs: {}, children: ["Question"]}, 
          {tag: "div", attrs: {className:"field"}, children: [
            {tag: "input", attrs: {type:"text", 
              value:ctrl.data().question, 
              onkeyup:function(event){
                ctrl.data().question = $(event.target).val()
              }}
            }
          ]}
        ]}, 
        
        {tag: "div", attrs: {className:"field"}, children: [
          {tag: "label", attrs: {}, children: ["Answers"]}, 
          {tag: "div", attrs: {className:"fields"}, children: [
            ctrl.data().answers.map(function(answer, index){
              return (
                {tag: "div", attrs: {className:"field"}, children: [
                  {tag: "input", attrs: {type:"text", value:answer, 
                    onkeyup:function(event){
                      ctrl.data().answers[index] = $(event.target).val()
                    }}
                  }
                ]}
              )
            })
          ]}
        ]}, 
        
        {tag: "div", attrs: {className:"field"}, children: [
          {tag: "label", attrs: {}, children: ["Key"]}, 
          {tag: "div", attrs: {className:"fields"}, children: [
            {tag: "div", attrs: {className:"field"}, children: [
              {tag: "input", attrs: {type:"text", value:ctrl.data().key, 
                 onkeyup:function(event){
                   ctrl.data().key = $(event.target).val()
                 }}
              }
            ]}
          ]}
        ]}
      ]}, 
      {tag: "div", attrs: {class:"ui button", tabindex:"0", 
        onclick:function(event){
          if(check(data)){
            var sendJson = $.extend(true, {}, ctrl.data())
            sendJson.answers = sendJson.answers.toString();
            sendJson.key = sendJson.key.toString();
            console.log(ctrl.data())
            $.ajax({
              type: "POST",
              url: "/api/admin/question/new",
              // The key needs to match your method's input parameter (case-sensitive).
              data: JSON.stringify(sendJson),
              contentType: "application/json; charset=utf-8",
              dataType: "text",
              success: function(data){
                alert(data);
              },
              failure: function(errMsg) {
                alert(errMsg);
              }
            });
            
          } else {
            alert("Missing some fields!")
          }
        }
      }, children: ["Submit question"]}
    ]}
  ]
};

var check = function(data){
  if(ctrl.data().question.length > 0 &&
      ctrl.data().answers[0].length > 0 &&
      ctrl.data().answers[1].length > 0 &&
      ctrl.data().answers[2].length > 0 &&
      ctrl.data().answers[3].length > 0 &&
      ctrl.data().key.length > 0
  ) {
    return true
  } else {
    return false
  }
}

module.exports = View;
},{}],7:[function(require,module,exports){
var View = function(ctrl){
  return [
    {tag: "div", attrs: {className:"ui segment"}, children: [
      {tag: "h2", attrs: {}, children: ["(type 2)Eg: Choose the right word for the given definition."]}, 
      {tag: "strong", attrs: {}, children: ["1. bad or hurting others"]}, " ", {tag: "br", attrs: {}}, 
      "a. afraid ", {tag: "br", attrs: {}}, 
      "b. clever ", {tag: "br", attrs: {}}, 
      "c. cruel ", {tag: "br", attrs: {}}, 
      "d. hunt"
    ]}
  ]
};


module.exports = View;
},{}],8:[function(require,module,exports){

var Home = require("./home/main.msx");
var NewQuestion = require("./newquestion/main.msx");
var EditQuestion = require("./editquestion/main.msx");

m.route.mode = 'hash';

m.route(document.body, "/", {
  "/": Home,
  "/new/:type": NewQuestion,
  "/edit/:id" : EditQuestion
});
},{"./editquestion/main.msx":5,"./home/main.msx":11,"./newquestion/main.msx":14}],9:[function(require,module,exports){

var Controller = function(){
  var ctrl = this;
  
  // $('.ui.sidebar').sidebar('show');
};


module.exports = Controller;
},{}],10:[function(require,module,exports){
var Sidebar = require('../_sidebar.msx');

var View = function(ctrl){
  return [
      Sidebar(ctrl),
      {tag: "div", attrs: {className:"pusher"}, children: [
        {tag: "div", attrs: {className:"ui segment"}, children: [
          {tag: "h2", attrs: {}, children: ["Admin page!"]}
        ]}
      ]}
  ]
};

module.exports = View;
},{"../_sidebar.msx":2}],11:[function(require,module,exports){
arguments[4][5][0].apply(exports,arguments)
},{"./_controller.msx":9,"./_view.msx":10}],12:[function(require,module,exports){
var Controller = function(){
  var ctrl = this;
  
  ctrl.data = m.prop(data());
  ctrl.type = m.route.param('type');
  
  // $('.ui.sidebar').sidebar('show');
};


var data = m.prop({
  typeNum: 1,
  question: "",
  answers: ["", "", "", ""],
  key: ""
});

module.exports = Controller;
},{}],13:[function(require,module,exports){
var Sidebar = require('../_sidebar.msx');
var Type1 = require('./type/1.msx');
var Type2 = require('./type/2.msx');
var list=[Type1, Type2];

var View = function(ctrl){
  return (
      {tag: "div", attrs: {}, children: [
        Sidebar(ctrl), 
        {tag: "div", attrs: {className:"pusher"}, children: [
            list[ctrl.type - 1](ctrl)
        ]}
      ]}
  )
};

module.exports = View;
},{"../_sidebar.msx":2,"./type/1.msx":15,"./type/2.msx":16}],14:[function(require,module,exports){
arguments[4][5][0].apply(exports,arguments)
},{"./_controller.msx":12,"./_view.msx":13}],15:[function(require,module,exports){


var View = function(ctrl){
  return [
    {tag: "div", attrs: {className:"ui  segment"}, children: [
      {tag: "h2", attrs: {}, children: ["(type 1)Eg: Choose the right word for the given definition."]}, 
      {tag: "strong", attrs: {}, children: ["1. bad or hurting others"]}, " ", {tag: "br", attrs: {}}, 
      "a. afraid ", {tag: "br", attrs: {}}, 
      "b. clever ", {tag: "br", attrs: {}}, 
      "c. cruel ", {tag: "br", attrs: {}}, 
      "d. hunt"
    ]},
    {tag: "div", attrs: {className:"ui segment"}, children: [
      {tag: "div", attrs: {className:"ui form"}, children: [
        {tag: "h4", attrs: {class:"ui dividing header"}, children: ["Type 1"]}, 
        {tag: "div", attrs: {className:"field"}, children: [
          {tag: "label", attrs: {}, children: ["Question"]}, 
          {tag: "div", attrs: {className:"field"}, children: [
            {tag: "input", attrs: {type:"text", 
              value:ctrl.data().question, 
              onkeyup:function(event){
                ctrl.data().question = $(event.target).val()
              }}
            }
          ]}
        ]}, 
        
        {tag: "div", attrs: {className:"field"}, children: [
          {tag: "label", attrs: {}, children: ["Answers"]}, 
          {tag: "div", attrs: {className:"fields"}, children: [
            ctrl.data().answers.map(function(answer, index){
              return (
                {tag: "div", attrs: {className:"field"}, children: [
                  {tag: "input", attrs: {type:"text", value:answer, 
                    onkeyup:function(event){
                      ctrl.data().answers[index] = $(event.target).val()
                    }}
                  }
                ]}
              )
            })
          ]}
        ]}, 
        
        {tag: "div", attrs: {className:"field"}, children: [
          {tag: "label", attrs: {}, children: ["Key"]}, 
          {tag: "div", attrs: {className:"fields"}, children: [
            {tag: "div", attrs: {className:"field"}, children: [
              {tag: "input", attrs: {type:"text", value:ctrl.data().key, 
                 onkeyup:function(event){
                   ctrl.data().key = $(event.target).val()
                 }}
              }
            ]}
          ]}
        ]}
      ]}, 
      {tag: "div", attrs: {class:"ui button", tabindex:"0", 
        onclick:function(event){
          if(check(ctrl.data)){
            var sendJson = $.extend(true, {}, ctrl.data())
            sendJson.answers = sendJson.answers.toString();
            sendJson.key = sendJson.key.toString();
            console.log(ctrl.data())
            $.ajax({
              type: "POST",
              url: "/api/admin/question/new",
              // The key needs to match your method's input parameter (case-sensitive).
              data: JSON.stringify(sendJson),
              contentType: "application/json; charset=utf-8",
              dataType: "text",
              success: function(data){
                m.route('/edit/' + data)
              },
              failure: function(errMsg) {
                alert(errMsg);
              }
            });
            
          } else {
            alert("Missing some fields!")
          }
        }
      }, children: ["Submit question"]}
    ]}
  ]
};

var check = function(data){
  if(data().question.length > 0 &&
      data().answers[0].length > 0 &&
      data().answers[1].length > 0 &&
      data().answers[2].length > 0 &&
      data().answers[3].length > 0 &&
      data().key.length > 0
  ) {
    return true
  } else {
    return false
  }
}

module.exports = View;
},{}],16:[function(require,module,exports){
module.exports=require(7)
},{}]},{},[8])