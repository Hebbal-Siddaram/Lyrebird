package moe.lyrebird.view.components.controlbar;

import org.springframework.stereotype.Component;
import moe.tristan.easyfxml.EasyFxml;
import moe.tristan.easyfxml.api.FxmlController;
import moe.tristan.easyfxml.model.beanmanagement.StageManager;
import moe.tristan.easyfxml.model.exception.ExceptionHandler;
import moe.tristan.easyfxml.util.Buttons;
import moe.tristan.easyfxml.util.Stages;
import moe.lyrebird.model.sessions.SessionManager;
import moe.lyrebird.view.components.Components;
import moe.lyrebird.view.screens.Screens;
import moe.lyrebird.view.screens.root.RootViewController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.util.List;

import static moe.lyrebird.view.screens.Screens.NEW_TWEET_VIEW;
import static moe.tristan.easyfxml.model.exception.ExceptionHandler.displayExceptionPane;

@Component
public class ControlBarController implements FxmlController {

    private static final Logger LOG = LoggerFactory.getLogger(ControlBarController.class);

    @FXML
    private BorderPane container;

    @FXML
    private Button tweetButton;

    @FXML
    private Button timelineViewButton;

    @FXML
    private Button mentionsViewButton;

    @FXML
    private Button directMessagesViewButton;

    @FXML
    private Button creditsButton;

    private final EasyFxml easyFxml;
    private final StageManager stageManager;
    private final RootViewController rootViewController;
    private final SessionManager sessionManager;

    private final Property<Button> currentViewButton;

    public ControlBarController(
            final EasyFxml easyFxml,
            final StageManager stageManager,
            final RootViewController rootViewController,
            final SessionManager sessionManager
    ) {
        this.easyFxml = easyFxml;
        this.stageManager = stageManager;
        this.rootViewController = rootViewController;
        this.sessionManager = sessionManager;
        this.currentViewButton = new SimpleObjectProperty<>(null);
    }

    @Override
    public void initialize() {
        currentViewButton.addListener((o, prev, current) -> {
            if (prev != null) {
                prev.setDisable(false);
            }
            current.setDisable(true);
        });

        Buttons.setOnClick(tweetButton, this::openTweetWindow);

        bindButtonToLoadingView(timelineViewButton, Components.TIMELINE);
        bindButtonToLoadingView(mentionsViewButton, Components.MENTIONS);
        bindButtonToLoadingView(directMessagesViewButton, Components.DIRECT_MESSAGES);

        Buttons.setOnClick(creditsButton, () ->
                easyFxml.loadNode(Screens.CREDITS_VIEW)
                        .orExceptionPane()
                        .map(pane -> Stages.stageOf("Credits", pane))
                        .andThen(Stages::scheduleDisplaying)
        );

        sessionManager.isLoggedInProperty().addListener((o, prev, cur) -> handleLogStatusChange(prev, cur));
        handleLogStatusChange(false, sessionManager.isLoggedInProperty().getValue());
        loadCurrentAccountPanel();
    }

    private void loadCurrentAccountPanel() {
        easyFxml.loadNode(Components.CURRENT_ACCOUNT)
                .getNode()
                .onSuccess(container::setTop)
                .onFailure(err -> displayExceptionPane(
                        "Could not load current user!",
                        "There was an error mapping the current session to a twitter account.",
                        err
                ));
    }

    private void openTweetWindow() {
        LOG.info("Opening new tweet stage...");
        final Pane tweetPane = this.easyFxml
                .loadNode(NEW_TWEET_VIEW)
                .getNode()
                .getOrElseGet(ExceptionHandler::fromThrowable);

        Stages.stageOf("Tweet", tweetPane)
              .thenCompose(Stages::scheduleDisplaying)
              .thenAccept(stage -> this.stageManager.registerSingle(NEW_TWEET_VIEW, stage))
              .thenRun(() -> LOG.info("New tweet stage opened !"));
    }

    private void handleLogStatusChange(final boolean previous, final boolean current) {
        final List<Button> loggedOnlyButtons = List.of(
                timelineViewButton,
                tweetButton,
                mentionsViewButton,
                directMessagesViewButton,
                tweetButton
        );
        loggedOnlyButtons.forEach(btn -> btn.setVisible(current));
        if (!previous && current) {
            timelineViewButton.fire();
        }
    }

    private void bindButtonToLoadingView(final Button button, final Components component) {
        Buttons.setOnClick(button, () -> {
            currentViewButton.setValue(button);
            rootViewController.setContent(component);
        });
    }
}
