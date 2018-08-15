/*
 *     Lyrebird, a free open-source cross-platform twitter client.
 *     Copyright (C) 2017-2018, Tristan Deloche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package moe.lyrebird.view.screens.media.display.photo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import moe.lyrebird.model.io.CachedMedia;
import moe.lyrebird.view.assets.ImageResources;
import moe.lyrebird.view.screens.media.display.MediaScreenController;

/**
 * This controller if managing detailed views of images.
 */
@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImageScreenController implements MediaScreenController {

    @FXML
    private AnchorPane container;

    @FXML
    private ImageView photoImageView;

    private final CachedMedia cachedMedia;

    private final Property<Image> imageProp = new SimpleObjectProperty<>(ImageResources.GENERAL_LOADING_REMOTE.getImage());

    @Autowired
    public ImageScreenController(final CachedMedia cachedMedia) {
        this.cachedMedia = cachedMedia;
        imageProp.addListener((observable, oldValue, newValue) -> bindViewSizeToParent());
    }

    @Override
    public void initialize() {
        photoImageView.imageProperty().bind(imageProp);
    }

    @Override
    public void handleMedia(final String mediaUrl) {
        imageProp.setValue(cachedMedia.loadImage(mediaUrl));
    }

    @Override
    public void bindViewSizeToParent() {
        container.setPrefWidth(imageProp.getValue().getWidth());
        container.setPrefHeight(imageProp.getValue().getHeight());
        photoImageView.fitHeightProperty().bind(container.heightProperty());
        photoImageView.fitWidthProperty().bind(container.widthProperty());
    }

}
