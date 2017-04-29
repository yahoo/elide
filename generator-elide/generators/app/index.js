/*
Copyright 2017, Yahoo Inc.

Licensed under the Apache License, Version 2.0

The use and distribution terms for this software are covered by the Apache License, Version 2.0 http://www.apache.org/licenses/LICENSE-2.0.html.
*/

const Generator = require('yeoman-generator');
const prompt = require('./prompt');
const model = require('./model');
const generator = require('./generator');
const misc = require('./misc.js');

class ElideGenerator extends Generator {
	constructor(args, opts) {
    super(args, opts);

    this.option('example');	// Option flag to generate an example
    this.option('create');	// Option flag to create a new project
    this.option('info');		// Option flag to show Elide Boot info
	}

	main() {
		if (this.options.example) {
			console.log('Generate an example');
			generator.generateExampleProject(this, 'com.yahoo.elide.example');
			console.log('Example project created in elide-blog-example directory');
		}
		else if (this.options.create) {
			console.log('Create a new project');
			prompt.createNewProject(this);
		}
		else if (this.options.model) {
			console.log('Add models to project');
			model.modelPrompt(this);
		}
		else if (this.options.info){
			misc.showInfo();
		}
		else {
			prompt.prompting(this);
		}
	}
};

module.exports = ElideGenerator
