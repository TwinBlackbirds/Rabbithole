# Scraper Template

A generic scraper template for Selenium apps built in Java. It also includes a pre-configured Maven setup as well as some custom tooling for logging and app configuration. 


### To use:

1. First, configure the database as necessary within `src/main/resources/hibernate.cfg.xml`.
1. Next, add your models and annotate them with JPA.
1. Add the mappings to the database configuration
1. Write necessary methods for specialized CRUD within the database driver (tbb.db.Driver.Sqlite)