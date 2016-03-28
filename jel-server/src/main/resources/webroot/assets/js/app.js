/* global Gauge */

(function () {
  'use strict';
  
  function createGauge(name, elementId) {
    return new Gauge({
      renderTo: elementId,
      width: 400,
      height: 400,
      glow: false,
      units: '°C',
      title: name,
      minValue: 0,
      maxValue: 55,
      majorTicks: ['0', '5', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'],
      minorTicks: 5,
      strokeTicks: false,
      highlights: [
        {from: 0, to: 22, color: 'rgba(0, 0, 255, .3)'},
        {from: 22, to: 24, color: 'rgba(0, 255, 0, .3)'},
        {from: 24, to: 55, color: 'rgba(255, 0, 0, .3)'}
      ],
      colors: {
        plate: '#222',
        majorTicks: '#f5f5f5',
        minorTicks: '#ddd',
        title: '#fff',
        units: '#ccc',
        numbers: '#eee',
        needle: {
          start: 'rgba(240, 128, 128, .75)',
          end: 'rgba(255, 160, 122, .75)',
          circle: {
            outerStart: '#333',
            outerEnd: '#111',
            innerStart: '#111',
            innerEnd: '#222'
          },
          shadowUp: true,
          shadowDown: false
        },
        circle: {
          shadow: false,
          outerStart: '#333',
          outerEnd: '#111',
          middleStart: '#222',
          middleEnd: '#111',
          innerStart: '#111',
          innerEnd: '#333'
        },
        valueBox: {
          rectStart: '#222',
          rectEnd: '#333',
          background: '#babab2',
          shadow: 'rgba(0, 0, 0, 1)'
        }
      },
      valueBox: {
        visible: true
      },
      valueText: {
        visible: true
      },
      animation: {
        delay: 10,
        duration: 1500,
        fn: 'linear'
      }
    });
  }

  var gauge = createGauge('Kök', '1446390193');
  gauge.draw();

  gauge = createGauge('Vardagsrum', '157310299');
  gauge.draw();

  gauge = createGauge('Hall/pannrum', '666657313');
  gauge.draw();

  gauge = createGauge('Arbetsrum', '302851082');
  gauge.draw();

  gauge = createGauge('Entré', '1544184166');
  gauge.draw();

  gauge = createGauge('Badrum', '92279315');
  gauge.draw();

  gauge = createGauge('Varmvatten ut', '775199740');
  gauge.draw();

  gauge = createGauge('Varmvatten in', '347706997');
  gauge.draw();

  var eb = new EventBus(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port + '/eventbus');
  eb.onopen = function () {
    // set a handler to receive a message
    eb.registerHandler('jel.eventbus.public', function (error, message) {
      if (error) {
        console.log('received an error: ' + error);
      } else {
        if (message && message.headers && message.headers.action) {
          switch (message.headers.action) {
            case 'DEVICE_NEWREADING':
              updateGauge(message.body);
              break;
            default:
              console.log('unsupported message action "' + message.headers.action + '"');
          }
        } else {
          console.log('received a message without action-header. Msg: ' + JSON.stringify(message));
        }
      }
    });
  };
  eb.onclose = function () {
    console.log('connection closed!');
  };

  /**
   *  Updated gauge with new readings.
   * @param {Object} message
   * @param {Object.String} message.id
   * @param {Object.String} message.time
   * @param {Object.String} message.value
   * @returns {undefined}
   */
  function updateGauge(message) {
    var gauge = Gauge.Collection.get(message.id);
    // Check that gauge exists for this device before we try to set its value.
    if (gauge) {
      gauge.setValue(message.value);
    }
  }
})();
