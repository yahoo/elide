# How to Contribute

When submitting a pull request (PR), please use the following guidelines:

- Make sure your code respects existing formatting conventions. In general, follow
  the same coding style as the code that you are modifying. If you are using
  IntelliJ, you can import our code style settings xml:
  [elide-intellij-codestyle.xml](https://github.com/yahoo/elide/raw/master/elide-intellij-codestyle.xml).
- Do add/update documentation appropriately for the change you are making.
- Bugfixes should include a unit test or integration test reproducing the issue.
- Do not use author tags/information in the code.
- Always include license header on each file your create. See [this example](https://github.com/yahoo/elide/blob/master/elide-core/src/main/java/com/yahoo/elide/Elide.java)
- Try to keep pull requests short and submit separate ones for unrelated
  features, but feel free to combine simple bugfixes/tests into one pull request.
- Keep the number of commits small and combine commits for related changes.
  Each commit should compile on its own and ideally pass tests.
- Keep formatting changes in separate commits to make code reviews easier and
  distinguish them from actual code changes.
