var sri = Math.random().toString(36).substring(2);

var userId = document.body.getAttribute("id");

var userName = document.body.getAttribute("name");
var mVersion = parseInt(document.body.getAttribute("mv"));
var mRVersion = parseInt(document.body.getAttribute("mv"));

var redraw = 0;

//var ws = new WebSocket("ws://188.166.254.203:9000/socket?sri=" + sri);
var ws = new WebSocket("ws://" + document.domain + ":9000/socket?sri=" + sri);
//var ws = new WebSocket("ws://localhost:9000/socket?sri=" + sri);
//var ws = new WebSocket("ws://192.168.1.25:9000/socket?sri=" + sri);

var pingData = function() {
  return JSON.stringify({
    t: "p",
    v: mVersion
  });
};

var sendData = function(t, d){
  return JSON.stringify({
    t: t,
    d: d
  })
};


var send = function (message, callback) {
  waitForConnection(function () {
    ws.send(message);
    if (typeof callback !== 'undefined') {
      callback();
    }
  }, 1000);
};

var waitForConnection = function (callback, interval) {
  if (ws.readyState === 1) {
    callback();
  } else {
    setTimeout(function () {
      waitForConnection(callback, interval);
    }, interval);
  }
};

setInterval(function(){
  send(pingData());
}, 1000);


ws.onopen = function(){
  console.log('WebSocket ok');
  send(sendData("get_onlines", ""));
};


ws.onclose = function(){
  alert("socket closed")
  console.log("socket closed")
};

ws.onerror = function(){
  alert("socket error")
  console.log("socket error")
}

var ctrl = {};
var data = {
  userOnline: [],
  user: {},
  chat: [],
  notify: {
    n: 0,
    notifyMessage: [],
    init: false,
    display: false
  }
};


var getPosChat = function(user, mv){
  var cv = (mv == undefined)?0:mv;
  var read = (mv == undefined);
  pos = -1;
  for(var len = 0; len < data.chat.length; len++){
    if(data.chat[len].user.id == user.id) {
      pos = len;
      return pos
    }
  }
  if(pos = -1){
    console.log("send inti_chat form getPosChat");
    data.chat.push({user: user, display: true, input: m.prop(''), init: false, hide: false, read: read, chat: []});
    send(sendData("init_chat", {w: user.id, cv: cv}));
    return (data.chat.length - 1)
  }
};

ctrl.listen = function(d){

  if(d.t === "n"){
    if(!data.notify.display) {
      var preNotify = data.notify.n;
      if (data.notify.display == false) data.notify.n = d.d;
      if (preNotify !== data.notify.n) m.redraw()
    }
  }

  if(d.t === "ul"){
    d.d.map(function(user){
      if(data.userOnline.indexOf(user) < 0) {
        data.userOnline.push(user);
      }
    });
    m.redraw();
  }

  if(d.t === "following_leaves"){
    data.userOnline.splice(data.userOnline.indexOf(d.d), 1);
    m.redraw();
  }

  if(d.t === "following_enters"){
    if(data.userOnline.indexOf(d.d) < 0) {
      data.userOnline.push(d.d);
      m.redraw();
    }
  }

  if(d.t === "nu"){
    data.user[d.d.id].name = d.d.n
    m.redraw();
  }

  if(d.t === "mes"){
    if(mVersion >= (d.d.v-1)){
      doMes(d);
      mVersion++;
      mRVersion ++;
    } else {
      var sendMes = { f: (mVersion+1), t: (d.d.v -1) };
      mVersion = d.d.v;

      setTimeout(function delayDoMes(){
        console.log("delay domess");
        if(mRVersion >= sendMes.t){
          doMes(d);
          mRVersion++;
        }else{
          setTimeout(delayDoMes, 200);
        }
      }, 100);

      console.log("SEEND REQUEST GET MISSING MESS: " + sendMes.f + "=>" + sendMes.t);
      send(sendData("gmm", sendMes));
      var gmm = setTimeout(function getMissMes(){
        if(mRVersion >= sendMes.t){
          clearTimeout(gmm)
        } else {
          console.log("SEEND REQUEST GET MISSING MESS: " + sendMes.f + "=>" + sendMes.t);
          console.log("current mes need:" + sendMes.t);
          send(sendData("gmm", sendMes));
          setTimeout(getMissMes, 1500)
        }
      }, 1500)

    }
  }

  if(d.t === "init_chat"){
    var listMes = d.d;
    if(listMes.length > 0){
      var user = (userId == d.d[0].f.id)?d.d[0].t:d.d[0].f;
      var pos = getPosChat(user);
      //console.log("init posssssssss:" + pos);
      if(data.chat[pos].chat.length <= 1) {
        d.d.map(function (mes) {
          //console.log("init_chat: " + mes.mv);
          //console.log("push:" + mes.mes);
          data.chat[pos].chat.push(mes)
        });
      } else {
        d.d.map(function(mes){
          if(mes.mv < data.chat[pos].chat[0].mv) data.chat[pos].chat.push(mes)
        })
      }
      data.chat[pos].chat.sort(sortByVer);
      m.redraw()
    }
  }

  if(d.t === "smm"){
      d.d.d.map(function(mes){
      var uid = (userId == mes.f)?mes.t:mes.f;
      var pos = getPosChat(uid);
      if(data.chat[pos].chat.length < 1) {
        d.d.d.map(function (mes) {
          data.chat[pos].chat.push(mes)
        });
      } else {
        d.d.d.map(function (mes) {
          console.log("smm: " + mes.mv + " --- " + data.chat[pos].chat[data.chat[pos].chat.length - 1].mv);
          if (mes.mv > data.chat[pos].chat[data.chat[pos].chat.length - 1].mv) data.chat[pos].chat.push(mes)
        })
      }
      if(mRVersion == d.d.f -1 ) mRVersion = d.d.t;
     });

      m.redraw()
  }

  if(d.t === "init_notify") {
    data.notify.notifyMessage = d.d;
    data.notify.init = true;
    m.redraw()
  }
};

function sortByVer(a,b) {
  //console.log("sorting " + a.mv + b.mv)
  if (a.mv < b.mv)
    return -1;
  if (a.mv > b.mv)
    return 1;
  return 0;
}




ws.onmessage = function (e) {
  var m = JSON.parse(e.data);
  ctrl.listen(m)
};

var doMes = function(d){
    var user = (userId == d.d.f.id)? d.d.t: d.d.f;
    var pos = getPosChat(user, d.d.mv);
    data.chat[pos].chat.push(
        {f: d.d.f, "mv": d.d.mv, "mes": d.d.m, "time": d.d.time}
    );
    if(data.chat[pos].display != true){
      data.chat[pos].display = true;
      data.chat[pos].hide = false;
    }
    if(userId !== d.d.f.id) {
      data.chat[pos].read = false;
    } else data.chat[pos].read = true;
    m.redraw();
};

$('body').on('click', '.relation_actions a.relation', function() {
  console.log("relation")
  var $a = $(this).addClass('processing');
  $.ajax({
    url: $a.attr('href'),
    type: 'post',
    success: function(html) {
      $a.parent().html(html);
    }
  });
  return false;
});