/* This is a private project. All rights reserved. */
// Edge can relaunch through its Windows compatibility layer, leaving Karma watching
// an exited launcher. Its retries then delete the live browser's temporary profile.
if (/[\\/]msedge(?:\.exe)?$/i.test(process.env.CHROME_BIN || '')) {
    config.customLaunchers = config.customLaunchers || {};
    config.customLaunchers.EdgeHeadless = {
        base: 'ChromeHeadless',
        flags: ['--edge-skip-compat-layer-relaunch', '--lang=en-US'],
    };
    config.browsers = config.browsers.map(browser => browser === 'ChromeHeadless' ? 'EdgeHeadless' : browser);
}

// UI selectors use the English resource catalogue on every host.
if (config.browsers.includes('ChromeHeadless')) {
    config.customLaunchers = config.customLaunchers || {};
    config.customLaunchers.EnglishChromeHeadless = {
        base: 'ChromeHeadless',
        flags: ['--lang=en-US'],
    };
    config.browsers = config.browsers.map(browser => browser === 'ChromeHeadless' ? 'EnglishChromeHeadless' : browser);
}
