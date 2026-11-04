-- Demo data (H2 MERGE: upsert, idempotent)
-- Priority: site=myapp > site=default, later-loaded profile wins

-- Shared base values (all sites, all profiles)
MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('default', 'default', 'app.title',   'APP', 'App Title',   'Default Application');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('default', 'default', 'app.version', 'APP', 'App Version', '1.0.0-DB');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('default', 'default', 'app.max-connections', 'DB', 'Max Connections', '10');

-- myapp site base values (site=myapp, profile=default)
MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'default', 'app.title',       'APP', 'App Title (Site)', 'Prop Demo App');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'default', 'app.description', 'APP', 'App Description', 'DB Property Source Demo');

-- myapp dev profile (site=myapp, profile=dev) - applied when active profile is dev
MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'dev', 'app.environment', 'APP', 'Environment', 'Development');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'dev', 'app.debug',       'APP', 'Debug Mode',  'true');

-- myapp prod profile (site=myapp, profile=prod) - applied when active profile is prod
MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'prod', 'app.environment', 'APP', 'Environment', 'Production');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'prod', 'app.debug',       'APP', 'Debug Mode',  'false');
