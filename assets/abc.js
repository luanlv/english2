(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){

require

var redraw = 0;
var rd = function(name){
  //console.log("redraw " + name)
};

var requestWithFeedback = function(args) {
  var data = m.prop();
  var completed = m.prop(false)
  var complete = function() {
    completed(true)
  }
  args.background = true;
  args.config = function(xhr) {
    xhr.timeout = 4000
    xhr.ontimeout = function() {
      complete();
      m.redraw()
    }
  }
  return {
    request: m.request(args).then(data).then(function(){complete();m.redraw()}),
    data: data,
    ready: completed
  }
}

var nav = {
  controller: function(){
    var ctrl = this;
    ctrl.displayNofity = function(){
      //if(data.notify.notifyMessage.length < 1){
        send(sendData("gnm", 0))
      //}
      data.notify.n = 0;
      data.notify.display = !data.notify.display
    };
  },
  view: function(ctrl){
    rd("nav");
    redraw++;
    return [
      m("a", {href: "/", config: m.route}, " Home |"),
      (userId.length>0)?(" ---Hello:" + userId) + "--- |":"",
      (userId.length>0)?m("a", {href: "/logout"}, "Logout"):"",
      (!userId.length>0)?m("a", {href: "/login"}, "Sign in |"):"",
      (!userId.length>0)?m("a", {href: "/signup"}, " Sign up"):"",
      (userId.length>0)?m('.notify', [
        m('.numNotify',{
          onclick: function(){
            ctrl.displayNofity()
          }
        }, data.notify.n),

        m('.notifyWr', [
          !data.notify.display?"":m('.inNotify', !data.notify.init?"LOADING":[
              data.notify.notifyMessage.map(function(mes){
                return m('.notifyMes', {
                  config: function(el){
                      $(el).click(function(){
                        data.notify.display = false;
                        var pos = getPosChat(mes.m.uid);
                        data.chat[pos].display = true;
                        data.chat[pos].hide = false;
                        if(mes.m.n > 0) data.chat[pos].read = false;
                        focusById(data.chat[pos].uid)
                      });
                  }
                }, [
                  m('.notifyName', getUser(mes.m.uid).name),
                  m('.mesNumber', " (" + mes.m.n+ ") "),
                  m('.lastMes', mes.m.lm.mes)
                ])
              })
          ])
        ])
      ]):"",
      m("", " " + "redraw: " + redraw)
    ]
  }
};


var Home = {
  controller: function() {
    var ctrl = this;
    ctrl.divs = m.prop([]);
    ctrl.divs([]);

  },
  view: function(ctrl) {
    rd("home");
    return [
        m('div', [
          ctrl.divs().map(function(item){
          return m('div.config', {id: item.id, v: item.v}, item.id)
        })
        ])
    ]
  }
};

var Dashboard = {
  controller: function() {
    console.log("run controller Dashboard");
    var ctrl = this;
    ctrl.server = initData.dashboard || {server: false};
    ctrl.request = (!ctrl.server.server)? requestWithFeedback({method: "GET", url: "/json"}) : {
      ready: function(){return true},
      data: m.prop(initData.dashboard.data)
    };
    ctrl.server.server = false;
  },
  view: function(ctrl) {
    rd("dashBoard");
    if(!ctrl.request.ready()) {
      return m('div', "LOADING !!!")
    } else {
      return [
        ctrl.request.data().map(function (u) {
          return m('div', u.name)
        })
      ]
    }
  }
};



var div = [{id: "home1", v: 1},{id: "home2", v: 5} ,{id:"home3", v: 10}];

var listId = [];
var flag = true;


function scrollBottom(element, isInit, context) {
  if(!isInit){
    element.scrollTop = element.scrollHeight;
  }
  var addLegth = (element.scrollHeight - context.prevHeight) || 0;
  if( element.scrollHeight - element.clientHeight - element.scrollTop < addLegth +  + 10 )
    element.scrollTop = element.scrollHeight;
  context.prevHeight = element.scrollHeight
}

var right = {
  controller: function(){
    var ctrl = this;
    m.redraw.strategy("diff");

    ctrl.makechat = function(user) {
      var pos = getPosChat(user);
      data.chat[pos].display = true;
      data.chat[pos].hide = false;
      focusById(data.chat[pos].user.id);
      m.redraw();
    };

    ctrl.toggleChat = function(num){
      data.chat[num].hide = !data.chat[num].hide;
    };

    ctrl.stopChat = function(num){
      data.chat[num].display = false;
    };

    ctrl.add = function (num) {
      var input = data.chat[num].input().trim();
      //input = input.replace(/\n/g, '');
      if (input) {
        send(sendData("m", {to: data.chat[num].user.id, mes: input}));

        //data.chat[num].chat.push({f: "u0", mes: input});
        //data.chat[num].chat.push({f: "u1", mes: "received: " + input});
      }
      data.chat[num].input('');
    };

    ctrl.save = function(e) {
      if (e.keyCode == 13) {
        //this causes a redraw, since event handlers active auto-redrawing by default
        ctrl.add()
      }
      else {
        //we don't care about other keys, so don't redraw
        m.redraw.strategy("none")
      }
    };

  },
  view: function(ctrl){
    rd("right");
    //console.log("right");
    return [
      m('#user-list', [
        m('.title-user-list', "USER ONLINE"),
        data.userOnline.map(function(user){
          return m('div.userOnline', {
            config: function(el){
              $(el).click(function(){
                if(userId.length > 0 && userId !== user.id) ctrl.makechat(user)
              });
            }
          }, user.name + ((userId.length > 0 && userId !== user.id)?"":" (you)"))
        }),
        m('.title-user-list', "USER ONLINE")
      ]),

      m('#dock-bot', [
      data.chat.map(function(chat, rank){
        return !chat.display?"":m('.chatWr' + (chat.hide?".w2":".w1"), [
          m('.chat-title2' + (chat.read?"":".unread"), {
            style: !chat.hide?"display: none":"",
            onclick: function(){ctrl.toggleChat(rank)}
          }, chat.user.name),
          m('.chatboxWr', {style: chat.hide?"display: none":""},
            [
                    m('.chat-title' + (chat.read?"":".unread"), {
                      onclick: function(){ctrl.toggleChat(rank)}
                    }, [
                      chat.user.name,
                      m('span.close-chat', {onclick: function(){ctrl.stopChat(rank)}}, "X")
                    ]),
                    m('.chat-box', {config: scrollBottom, onclick: function(){markRead(rank)}}, [
                      chat.chat.map(function(item, num){
                        return [
                          chat.init?m('.loading_chat', "Loading previous ..."):"",
                          m('div'+ ((item.f.id == userId)?".comment-left": ".comment-right"),
                              m('.mes', item.mes))
                        ]
                      })
                    ])
                    ,
                    m('textarea.auto-size.new-comment#' + (chat.user.id) + '[rows=1]', {
                      style: chat.hide?"display: none":"",
                      onfocus: function(){markRead(rank)},
                      //onselect: function(){markRead(rank)},
                      config: function (element, isInit, ctx) {
                          $(element).textareaAutoSize();
                          $(element).attrchange({
                            //trackValues: true,
                            callback: function (event) {
                              var prev = element.previousSibling
                              var $prev = $(prev);
                              if($prev.scrollTop() + $prev.innerHeight() >= element.previousSibling.scrollHeight){
                                $prev.css('height', 271 - $(element).outerHeight());
                                prev.scrollTop = prev.scrollHeight;
                              } else {
                                $prev.css('height', 271 - $(element).outerHeight())
                              }

                            }
                          });
                          element.value = data.chat[rank].input();
                      },
                      onkeypress: function(e){
                        if(e.keyCode == 13 && !e.shiftKey) {
                          if (data.chat[rank].input().length < 1) {
                            return false;
                          } else {
                            var source = e.target || e.srcElement;
                            var prev = source.previousSibling;
                            prev.scrollTop = prev.scrollHeight;
                            ctrl.add(rank);
                            return false;
                          }
                        }else{
                          m.redraw.strategy("none")
                          if(e.keyCode == 13 && e.shiftKey && data.chat[rank].input().length < 1)
                            return false;
                        }
                      },
                      value: data.chat[rank].input(),
                      oninput: setsVal(data.chat[rank].input)
                    })
            ])]
        ) // .right end
        }
      )
    ])
    ]
  }
};

var Loading = {
  controller: function(){

  },
  view: function(){
    console.log("render loading!!");
    return m('', 'LOADING')
  }
};


function route( sub ){
  return {
    controller : function(){
      m.redraw.strategy( 'diff' );

      return new sub.controller();
    },
    view : sub.view
  }
}

function setsVal(callback) {
  return function(e) {
    m.withAttr("value", callback)(e);
    m.redraw.strategy("none")
  }
}


var getUser = function(name){
  if(data.user[name] !== undefined){
    return data.user[name]
  } else {
    data.user[name] = {};
    send(sendData("gn", name));
    setTimeout(function getName(){
      if(data.user[name]  === undefined){
        setTimeout(getName, 2000);
      }
    }, 2000);
    return {name: "...."}
  }
};

var markRead = function(rank){
  console.log("run markread ")
  if(data.chat[rank].read === false){
    if(data.chat[rank].chat[data.chat[rank].chat.length - 1].f.id == data.chat[rank].user.id) {
      send(sendData("mr", {
        uid: data.chat[rank].user.id,
        mv: data.chat[rank].chat[data.chat[rank].chat.length - 1].mv
      }));
      data.chat[rank].read = true;
    }
  }
};

var focusById = function(uid){
  setTimeout(function focusComment(){
    if(document.getElementById(uid) != undefined){
      console.log("focuss ok")
      document.getElementById(uid).focus();
    } else {
      console.log("focuss error")
      setTimeout(focusComment, 100)
    }
  }, 100);
}

m.mount(document.getElementById('nav'), nav);
m.mount(document.getElementById('app'), Loading);
m.mount(document.getElementById('rightContainer'), right);
},{}]},{},[1]);
