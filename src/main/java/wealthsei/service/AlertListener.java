package wealthsei.service;

import wealthsei.domain.Alert;

/**
 * Anything that wants to be told about alerts (Observer pattern). The GUI and the CLI each
 * implement this, so the services never depend on a screen.
 */
public interface AlertListener {
    void onAlert(Alert alert);
}
