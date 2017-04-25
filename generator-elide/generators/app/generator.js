// TODO: to be refactored to use generator
const generateExampleProject = (yo, groupId) => {
  const file = groupId.split('.').join('/');

  const srcJavaPath = 'src/main/java';
  const srcResourcePath = 'src/main/resources';

  const dstRootPath = 'elide-blog-example';
  const dstJavaPath = `${dstRootPath}/src/main/java`;
  const dstResourcePath = `${dstRootPath}/src/main/resources`;

  yo.fs.copyTpl(
    yo.templatePath(`${srcJavaPath}/Main.java`),
    yo.destinationPath(`${dstJavaPath}/${file}/Main.java`),
    { groupId }
  );
  yo.fs.copyTpl(
    yo.templatePath(`${srcJavaPath}/ElideResourceConfig.java`),
    yo.destinationPath(`${dstJavaPath}/${file}/ElideResourceConfig.java`),
    { groupId }
  );

  // Init the models folder blog-example
  yo.fs.copyTpl(
    yo.templatePath('blog-example/Comment.java'),
    yo.destinationPath('elide-blog-example/src/main/java/' + file + '/models/Comment.java')
  );
  yo.fs.copyTpl(
    yo.templatePath('blog-example/Post.java'),
    yo.destinationPath('elide-blog-example/src/main/java/' + file + '/models/Post.java')
  );
  yo.fs.copyTpl(
    yo.templatePath('blog-example/Role.java'),
    yo.destinationPath('elide-blog-example/src/main/java/' + file + '/models/Role.java')
  );
  yo.fs.copyTpl(
    yo.templatePath('blog-example/User.java'),
    yo.destinationPath('elide-blog-example/src/main/java/' + file + '/models/User.java')
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
    yo.templatePath('blog-example/load_blog.sh'),
    yo.destinationPath('elide-blog-example/src/main/scripts/load_blog.sh')
  );

  yo.fs.copyTpl(
    yo.templatePath('pom.xml'),
    yo.destinationPath('elide-blog-example/pom.xml'),
    {
      artifactId: 'elide-blog-example',
      groupId: 'com.yahoo.elide',
      projectName: 'Elide Example: Hibernate5 API with Security',
      version: '3.0.5-SNAPSHOT',
      description: 'Elide example using javax.persistence, MySQL and Elide Security'
    }
  );
  yo.fs.copyTpl(
    yo.templatePath('.gitignore'),
    yo.destinationPath(`${dstRootPath}/.gitignore`)
  );
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
