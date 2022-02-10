# Twitter CLI

A very stupid Twitter Client on the command line. It only has two functions: fetch the timeline and post a tweet.
Create a `secrets.edn` file following the template from secrets.example.edn. This file holds authentication keys and access tokens.
The secrects file lets you add multiple accounts.

### requirements

JVM and [Clojure CLI](https://clojure.org/guides/getting_started)

### Usage

1. Fetch the timeline for an account:
   ```
   clj -A:twitter account1 fetch
   ```
   
2. Post a tweet:
   ```
   clj -A:twitter account2 fetch "Hello, World"
   ```
