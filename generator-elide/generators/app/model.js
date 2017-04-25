const generator = require('./generator');

const srcJavaPath = 'src/main/java';

const choices = [
  'String',
  'Int',
  'Short',
  'Float',
  'Double',
  'Long',
  'Long long',
  'Boolean',
  'Char'
];

const models = [];

let newModelAttributes = {
  name: '',
  fields: []
};

const fieldPrompt = (yo, pomObj) => {
	return yo.prompt([{
		type: 'input',
		name: 'name',
		message: 'Field name?'
	}, {
		name: 'type',
		message: 'What type?',
		type: 'list',
		choices
	}]).then((model) => {
        if (model.type != "String") {
            model.type = model.type.toLowerCase();
        }
        const { name, type } = model;
		newModelAttributes.fields.push({ name, type });
		yo.prompt([{
			type: 'confirm',
			name: 'continue',
			message: 'Add another field?'
		}]).then((response) => {
			if (response.continue) {
				fieldPrompt(yo, pomObj);
			} else {
				models.push(newModelAttributes);
				choices.push(newModelAttributes.name);
				newModelAttributes = {
          name: '',
          fields: []
        };
				yo.prompt([{
						type: 'confirm',
						name: 'addAnother',
						message: 'Add another model?'
				}]).then((answer) => {
          answer.addAnother ? modelPrompt(yo, pomObj) : createModels(yo, pomObj);
				});
			}
		});
	});
}

const modelPrompt = (yo, pomObj) => {
	yo.prompt([{
		type: 'input',
		name: 'name',
		message: 'Model name?'
	},]).then((model) => {
		newModelAttributes.name = model.name;
		fieldPrompt(yo, pomObj);
	});
}

const createModels = (yo, pomObj) => {
	const file = pomObj.groupId.split('.').join('/')
	models.forEach((model) => {
    const newModel = Object.assign(model, { groupId: pomObj.groupId });
    yo.fs.copyTpl(
      yo.templatePath(`${srcJavaPath}/models/Model.java`),
      yo.destinationPath(`${pomObj.artifactId}/src/main/java/${file}/models/${model.name}.java`),
      newModel
    );
  });

  generator.generateNewProject(yo, pomObj);
}

module.exports = {
  modelPrompt,
  newModelAttributes
}
