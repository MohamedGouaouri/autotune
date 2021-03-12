# **Autotune tests**

## Test Scenarios
- IT Admin adds Autotune object (application config for autotune to tune the app)
- SME adds / modifies Autotune layer
- SME adds / modifies Autotune tunable
- Autotune REST APIs


### Below is a brief description of the tests :

- **Application autotune yaml tests**

  Here we validate if a user is able to add an autotune object for the application that needs to be monitored by autotune and if autotune rejects the invalid autotune yamls with appropriate error messages.
   The test does the following:
   - Deploys autotune and its dependencies using the [deploy script](https://github.com/kruize/autotune/blob/master/deploy.sh) from the autotune repo
   - Applies the application autotune yaml 
   - Checks if autotune validates the yaml 

- **Autotune config yaml tests**

  Here we validate if a user can add a new autotune layer configuration with the required tunables or modify the existing layer configuration. We also check if autotune handles invalid configurations well giving out appropriate error messages to the users.
   The test does the following:
   - Deploys autotune and its dependencies using the deploy script from the autotune repo
   - Applies the autotune config yaml 
   - Checks if autotune validates the yaml

- **Basic API tests**

  Here we validate all the [Autotune REST APIs](https://github.com/kruize/autotune/blob/master/design/API.md)
  The test does the following:
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Deploys multiple instances of spring petclinic application 
  - Applies the autotune application yamls for the petclinic deployments
  - Validates the JSON output for all the Autotune REST APIs 

- **Modify autotune config tests**

  Here we modify the layer config and validate the listAutotuneTunables Autotune REST API
  The test does the following:
  - Deploys autotune and its dependencies using the deploy script from the autotune repo
  - Modify the layer config and apply
  - Validate if the modified config is reflected in the JSON output from listAutotuneTunables Autotune API  


## Supported Clusters
- Minikube

## Prerequisites for running the tests:

- Minikube setup 
- Tools like docker, kubectl, and jq 
- Install Prometheus on minikube

To install prometheus on minikube, run the below command:

```
<AUTOTUNE_REPO>/scripts/prometheus_on_minikube.sh -as
```

Clone the kruize/benchmarks repo using the below command:

```
git clone https://github.com/kruize/benchmarks.git

```

## To run the tests:

First, cleanup any previous instances of autotune using the below command:

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -t
```

Use the below command to test :

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube -r [location of benchmarks]  [-i autotune image] [--tctype=functional] [--testsuite=Group of tests that you want to perform] [--testcase=Particular test case that you want to test] [-n namespace] [--resultsdir=results directory] 
```

Where values for test_autotune.sh are:

```
usage: test_autotune.sh [ -c ] : cluster type. Supported type - minikube
                        [ -i ] : optional. Autotune docker image to be used for testing, default - kruize/autotune:test
			[ -r ] : Location of benchmarks
			[ -- tctype ] : optional. Testcases type to run, default is functional (runs all functional tests)
			[ -- testsuite ] : Testsuite to run. Use testsuite=help, to list the supported testsuites
			[ --testcase ] : Testcase to run. Use testcase=help along with the testsuite name to list the supported testcases in that testsuite
			[ -n ] : optional. Namespace to deploy autotune
			[ --resultsdir ] : optional. Results directory location, by default it creates the results directory in current working directory

Note: If you want to run a particular testcase then it is mandatory to specify the testsuite

```

For example,

```
<AUTOTUNE_REPO>/tests/test_autotune.sh -c minikube --tctype=functional --testsuite=app_autotune_yaml_tests --testcase=sla_class -r /home/benchmarks --resultsdir=/home/results
```

