var m = require('mithril');
var util = require('../util');
var defined = util.defined;
var classSet = require('chessground').util.classSet;

var gaugeLast = 0;
var gaugeTicks = [];
for (var i = 1; i < 8; i++) gaugeTicks.push(m(i === 4 ? 'tick.zero' : 'tick', {
  style: {
    height: (i * 12.5) + '%'
  }
}));

function localEvalInfo(ctrl, evs) {
  if (!evs.client) {
    if (evs.server && ctrl.nextNodeBest()) return 'Using server analysis';
    return 'Loading engine...';
  }
  if (evs.client.dict) return 'Book move';
  var t = 'Depth ' + (evs.client.depth || 0) + '/' + evs.client.maxDepth;
  if (evs.client.nps) t += ', ' + Math.round(evs.client.nps / 1000) + ' knodes/s';
  return t;
}

module.exports = {
  renderGauge: function(ctrl) {
    if (ctrl.ongoing || !ctrl.showEvalGauge()) return;
    var eval, evs = ctrl.currentEvals();
    if (evs) {
      if (defined(evs.fav.cp))
        eval = 2 / (1 + Math.exp(-0.005 * evs.fav.cp)) - 1;
      else
        eval = evs.fav.mate > 0 ? 1 : -1;
      gaugeLast = eval;
    } else eval = gaugeLast;
    var height = 100 - (eval + 1) * 50;
    return m('div', {
      class: classSet({
        eval_gauge: true,
        empty: eval === null,
        reverse: ctrl.data.orientation === 'black'
      })
    }, [
      m('div', {
        class: 'black',
        style: {
          height: height + '%'
        }
      }),
      gaugeTicks
    ]);
  },
  renderCeval: function(ctrl) {
    if (!ctrl.ceval.allowed() || !ctrl.ceval.possible()) return;
    var enabled = ctrl.ceval.enabled();
    var evs = ctrl.currentEvals() || {};
    var pearl, percent;
    if (defined(evs.fav) && defined(evs.fav.cp)) {
      pearl = util.renderEval(evs.fav.cp);
      percent = ctrl.nextNodeBest() ?
        100 :
        (evs.client ? Math.min(100, Math.round(100 * evs.client.depth / evs.client.maxDepth)) : 0)
    } else if (defined(evs.fav) && defined(evs.fav.mate)) {
      pearl = '#' + evs.fav.mate;
      percent = 100;
    } else if (ctrl.gameOver()) {
      pearl = '-';
      percent = 0;
    } else {
      pearl = m('span.cpu', 'CPU');
      percent = 0;
    }
    return m('div.ceval_box',
      enabled ? m('div.bar', m('span', {
        style: {
          width: percent + '%'
        },
        config: function(el, isUpdate, ctx) {
          // reinsert the node to avoid downward animation
          if (isUpdate && ctx.percent > percent) {
            var p = el.parentNode;
            p.removeChild(el);
            p.appendChild(el);
          }
          ctx.percent = percent;
        }
      })) : null,
      enabled ? [
        m('pearl', pearl),
        m('div.engine', [
          'Local ' + util.aiName(ctrl.data.game.variant),
          m('span.info', localEvalInfo(ctrl, evs))
        ])
      ] : m('help',
        'Local computer evaluation',
        m('br'),
        'for variation analysis'
      ),
      m('div.switch', [
        m('input', {
          id: 'analyse-toggle-ceval',
          class: 'cmn-toggle cmn-toggle-round',
          type: 'checkbox',
          checked: enabled,
          config: util.bindOnce('change', ctrl.toggleCeval)
        }),
        m('label', {
          'for': 'analyse-toggle-ceval'
        })
      ])
    );
  }
};
