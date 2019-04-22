# How to Contribute

When submitting a pull request (PR), please use the following guidelines:

- Make sure your code respects existing formatting conventions. In general, follow
  the same coding style as the code that you are modifying. If you are using
  IntelliJ, you can import our code style settings xml:
  [elide-intellij-codestyle.xml](https://github.com/yahoo/elide/raw/master/elide-intellij-codestyle.xml).
- Do add/update documentation appropriately for the change you are making.  Documentation for Elide is maintained in [another repo](https://github.com/yahoo/elide-doc).
- Bugfixes should include a unit test or integration test reproducing the issue.
- Do not use author tags/information in the code.
- Always include license header on each file your create. See [this example](https://github.com/yahoo/elide/blob/master/elide-core/src/main/java/com/yahoo/elide/Elide.java)
- Try to keep pull requests short and submit separate ones for unrelated
  features, but feel free to combine simple bugfixes/tests into one pull request.
- Keep the number of commits small and combine commits for related changes.
  Each commit should compile on its own and ideally pass tests.
- Update the [changelog](https://github.com/yahoo/elide/blob/master/changelog.md) with a description of the changes.
- Keep formatting changes in separate commits to make code reviews easier and
  distinguish them from actual code changes.
- All code must receive at least one positive review from _another_ committer to be merged to master.

# Community

While anyone is free to open pull requests against the project, Elide has two defined roles:
- *Committers* - Are the gatekeepers of the repository.  They approve pull requests and have write access to merge against master or other mainline branches.  Committers also help set the direction for the project by deciding on major enhancements or design changes.
- *Admins* - Are responsible for publishing Elide releases.  They are also responsible for maintaining the project's community process and webpage.

The current committer list includes:
| Name             | Organization  | Admin | 
| ---------------- | ------------- |-------|
| Dennis McWherter | Amazon        | X     |
| Aaron Klish      | Yahoo         | X     |
| William Cekan    | Yahoo         | X     |
| Clay Reimann     | Yahoo         | X     |
| Dan Chen         | Yahoo         |       |
| Jon Kilroy       | Yahoo         |       |

## Becoming A Committer

Committers are contributors who:
- Have demonstrated a good understanding of Elide through contribution of code, documentation, and forum discussion.
- Follow the [code of conduct](https://www.apache.org/foundation/policies/conduct.html) when communicating within the community.

New committers are either nominated by another committer or can request to become a committer.  Nominated committers must have a net total of 
at least three positive votes from other committers to gain committer status and be added to this page.  Voting will be public and will take place in
the _general_ forum on Elide's slack.

# Releases

Releases are published manually from master by project Admins.  

When a new release is published, it is first published as a release candidate.  The candidate will be available for up to 1 week for testing from the community before
it is marked official.  During that week, the official release can be blocked by a detailed github issue filed against the release candidate.  
If the issue is considered significant by the project admins, the release is blocked until a new release candidate can be proposed.

Candidate releases will be announced in the _general_ forum of Elide's slack.

## Release Labels

Elide releases follow [semantic versioning](https://semver.org/).  

Release candidates are marked by appending the semantic version with a hyphen (-), the text 'RC', and a number indicating the candidate number.
For example, 4.14.3-RC2 is the second release candidate for the 4.14.3 release.
