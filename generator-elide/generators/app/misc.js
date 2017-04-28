/*
Copyright 2017, Yahoo Inc.

Licensed under the Apache License, Version 2.0

The use and distribution terms for this software are covered by the Apache License, Version 2.0 http://www.apache.org/licenses/LICENSE-2.0.html.
*/

const showInfo = () => {
  const logo = `
 _____ _ _   _        _____         _
|   __| |_|_| |___   | __  |___ ___| |_
|   __| | | . | -_|  | __ -| . | . |  _|
|_____|_|_|___|___|  |_____|___|___|_|
                                  v.1.0`;
  console.log(logo);

  // TODO: update info below
  console.log('_______________________________________________________________');
  console.log('|Elide Boot is a command line interface (CLI) for Yahoo! Elide|');
  console.log('|Version: 1.0.0                                               |');
  console.log('|GitHub Repo: https://github.com/shaneneary/generator-elide   |');
  console.log('---------------------------------------------------------------');
}

module.exports = {
  showInfo
}
