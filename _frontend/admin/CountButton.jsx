"use strict";

import React from 'react';

export default class CountButton extends React.Component {
  constructor(props) {
    super(props);
  }
  render() {
    return (
        <div>
          <input type="button" value="カウント" onClick={this.props.onClick} />
        </div>
    );
  }
}