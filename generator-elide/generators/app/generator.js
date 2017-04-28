const generateExampleProject = (yo, groupId) => {
  var pomObj = { artifactId: 'elide-blog-example', 
    groupId: 'com.yahoo.elide', 
    projectName: 'Elide Example: Hibernate5 API with Security', 
    version: '3.0.5-SNAPSHOT', 
    description: 'Elide example using javax.persistence, MySQL and Elide Security'
  };
  generateNewProject(yo, pomObj);
}

const generateNewProject = (yo, pomObj) => {
  const file = pomObj.groupId.split('.').join('/');

  const srcJavaPath = 'src/main/java';
  const srcResourcePath = 'src/main/resources';

  const dstRootPath = pomObj.artifactId;
  const dstJavaPath = `${dstRootPath}/src/main/java`;
  const dstResourcePath = `${dstRootPath}/src/main/resources`;

  yo.fs.copyTpl(
    yo.templatePath(`${srcJavaPath}/Main.java`),
    yo.destinationPath(`${dstJavaPath}/${file}/Main.java`),
    { groupId: pomObj.groupId }
  );
  yo.fs.copyTpl(
    yo.templatePath(`${srcJavaPath}/ElideResourceConfig.java`),
    yo.destinationPath(`${dstJavaPath}/${file}/ElideResourceConfig.java`),
    { groupId: pomObj.groupId }
  );
  yo.fs.copyTpl(
    yo.templatePath(`${srcResourcePath}/hibernate.cfg.xml`),
    yo.destinationPath(`${dstResourcePath}/hibernate.cfg.xml`)
  );
  yo.fs.copyTpl(
    yo.templatePath(`${srcResourcePath}/log4j2.xml`),
    yo.destinationPath(`${dstResourcePath}/log4j2.xml`)
  );
  yo.fs.copyTpl(
    yo.templatePath('pom.xml'),
    yo.destinationPath(`${dstRootPath}/pom.xml`),
    pomObj
  );
  yo.fs.copyTpl(
    yo.templatePath('.gitignore'),
    yo.destinationPath(`${dstRootPath}/.gitignore`)
  );
}

module.exports = {
  generateExampleProject,
  generateNewProject
}
