"use strict";

import React from 'react';

const IMAGES = ["https://facebook.github.io/react/img/logo.svg", "https://jp.vuejs.org/images/logo.png", "http://riotjs.com/img/logo/riot240x.png"];
export default class CountImage extends React.Component {
  constructor(props) {
    super(props);
  }
  render() {
    let n = this.props.number % 3;
    return (
        <div>
          <img src={IMAGES[n]} />
        </div>
    );
  }
}