# Development

For more details on the [code structure here](code_overview.md)

## Releases

Releases can be done via github actions and the built jar and users can download it on the [github release page](https://github.com/ratschlab/medical-reports-deidentification/releases)

1. Adapt the version in the `<version>` section of [maven config](/deidentifier-pipeline/pom.xml)
2. merge this change into main
3. create a tag locally on your computer using `git tag v[version]`, e.g. `git tag v1.0.0`
4. push tag using `git push origin v1.0.0` (adapt to the version)
5. If the github actions workflow ran successfully, you can edit the release as necessary on the [github release page](https://github.com/ratschlab/medical-reports-deidentification/releases) and switch it from Pre-release to release
  (remove the flag `This is a pre-release`)

Note, that there is a danger that the version in `pom.xml` and in the git tag don't match. This could/should be streamlined in the future.
