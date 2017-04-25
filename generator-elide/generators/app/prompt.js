const generator = require('./generator');
const model = require('./model');
const misc = require('./misc.js');

// Main prompt when running 'yo elide'
const prompting = (yo) => {
  yo.prompt([{
    type: 'list',
    name: 'command',
    message: 'Choose an option below',
    choices: [ 'Try an example', 'Create a new project', 'Info & Contact' ]
  }]).then((answers) => {
    switch (answers.command) {
      case 'Try an example':
        generator.generateExampleProject(yo, 'com.yahoo.elide.example');
        console.log('Example project created in the elide-blog-example directory');
        break;
      case 'Create a new project':
        createNewProject(yo);
        break;
      case 'Info & Contact':
        misc.showInfo();
        break;
    }
  });
}

// Create new project prompt
const createNewProject = (yo) => {
  yo.prompt([{
    type: 'input',
    name: 'groupId',
    message: 'groupId?'
  }, {
    type: 'input',
    name: 'artifactId',
    message: 'artifactId?'
  }, {
    type: 'input',
    name: 'projectName',
    message: 'Project name?'
  }, {
    type: 'input',
    name: 'description',
    message: 'Description?'
  }, {
    type: 'input',
    name: 'version',
    message: 'Version?'
  }, {
    type: 'input',
    name: 'author',
    message: 'Author?'
  }, {
    type: 'input',
    name: 'license',
    message: 'License?'
  }, {
    type: 'confirm',
    name: 'model',
    message: 'Add a model and fields?'
  }]).then((answers) => {
    const { artifactId, groupId, projectName, version, description } = answers;

    if (answers.model) {
      model.modelPrompt(yo, { artifactId, groupId, projectName, version, description });
    } else {
      generator.generateNewProject(yo, { artifactId, groupId, projectName, version, description });
    }
  });
}

module.exports = {
  prompting,
  createNewProject
}
