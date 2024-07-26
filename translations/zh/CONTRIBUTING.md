# 开发 Elide

提交 pull request (PR) 时，请遵循如下要求：

- 代码必须和现有的代码风格一致。修改代码的话，新的代码要和之前写好的风格一致。如果您使用 IntelliJ，
  可以使用我们的代码风格配置文件：[elide-intellij-codestyle.xml](https://github.com/paion-data/elide/raw/master/elide-intellij-codestyle.xml).
- 修 Bug 需要有单元测试和集成测试，测试的内容必须是和 bug 相关的。
- 请勿在代码中包含 `author` 相关的 Javadoc 或信息。
- 每个代码文件必须包含开源许可抬头。详见[举例](https://github.com/paion-data/elide/blob/master/elide-core/src/main/java/com/paiondata/elide/Elide.java)
- 请尽量控制 pull requests 的大小，不用的问题请分开提交 pull requests，单独的小 bug 修复和测试除外。
- 请尽量减少 commit 的数量，相关的 commits 请和合并。每个 commit 需要完全通过测试。
- 代码风格小时需要放入单独的 commit，这样可以简化代码审核，便于与代码修改区分开来。
- 请严格遵循[开发守则](Code-Of-Conduct.md)
