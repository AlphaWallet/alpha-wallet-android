### Theming
The attributes defined in [`values/themes.xml`]() exist for two primary reasons:
* **Provide default values for colors and styles**
   * **Colors**
     Most native components derive their colors from one of the standard color references. For instance, the default status bar color is derived from `colorPrimaryDark`. Meanwhile, the activated state of a checkbox is derived from `colorControlActivated`.
   * **Styles**
     The default `toolbarStyle` is set to `Aw.Component.Toolbar`, which means if you inflate a `MaterialToolbar`, it is automatically styled as an AlphaWallet-themed toolbar.

  By having these default values specified in the theme, developers no longer need to manually style most native components, effectively reducing development time and improving UX consistency.

* **Allow flexibility for theme overlays**
  If colors are derived from these references, we can easily apply a theme overlay over a specific layout (ex. Dark overlay on a dialog even if the app is set to Light mode)

### Styles and Typography
Along with the color references in `themes.xml`, the resources defined in [`values/styles.xml`]() and [`values/typography.xml`]() allow developers to quickly new build layouts by accessing predefined styles for the most commonly used components.

Example 1: Creating a label
```xml
    <TextView 
        style="@style/Aw.Typography.Label"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
```

Example 2: Creating a separator
```xml
    <View style="@style/Aw.Component.Separator" />
```

Example 3: Creating a secondary button
```xml
    <com.google.android.material.button.MaterialButton
        style="@style/Aw.Component.Button.Secondary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
```

### Using color references
Using color references is the preferred way of applying colors to your components. There are several instances you want to use them:

1. **Creating new styles**
   When creating a style for a new component, you may want to use the color references whenever applicable. This allows flexibility for theme overlays.

   Example:
    ```xml
    <style name="Aw.Component.NewComponent">
        <item name="android:background">?colorPrimary</item>
        <item name="android:textColor">?colorOnPrimary</item>
    </style>
    ```

2. **Background drawables**
   You can use these color references directly onto your background drawables.

   Example:
    ```xml
    <shape android:shape="rectangle">
        <solid android:color="?colorSurface" />
    </shape>
    ```

3. **Directly onto a view (discouraged)**
   Before you do this, consider creating a new style for your component instead, especially if you think that it could be reusable in other layouts.

   Example:
    ```xml
    <TextView
        android:textColor="?colorPrimary" 
        ... />
    ```

4. **Programmatically (discouraged)**
   Programmatically creating views often signal an inefficiency, but there are a few instances where this is unavoidable. To access a color reference, use `Utils.getColorFromAttr(context, resId)`.

   Example:
    ```java
        int color = Utils.getColorFromAttr(getContext(), R.attr.colorPrimary);
        textView.setTextColor(color);
    ```

### Creating new color references
You may need to create a new color reference if any of the existing colors do not satisfy your requirements. In that case, you need to do the following:
1. Create a new entry in [`attrs.xml`]()
   Example:
    ```xml
        <attr name="newColorReference" format="reference" />
    ```
   > Tip: Keep in mind that these references are meant to be reused by other components that may share similar properties, so try to be moderately descriptive with the name.

2. Provide corresponding values in `colors.xml` and `night/colors.xml`
   Example:
    ```xml
        <item name="color_reference">@color/flamingo</item>
    ```
3. Add the new color reference to your theme
   Example:
    ```xml
        <style name="AppTheme">
            ...
            <item name="newColorReference">@color/color_reference</item>
        </style>
    ```
4. Apply the new color reference to your style or component
   Example:
    ```xml
        <style name="AppTheme">
            ...
            <item name="android:background">?newColorReference</item>
        </style>
    ```

### Rebranding or customizing the theme
AlphaWallet is one of the best Ethereum wallets to fork, so we have provided an easy way for you to customize it.

##### Changing fonts
In `app/src/main/res/font`, simply replace the following files with your preferred font:

* font_light.ttf
* font_regular.ttf
* font_semibold.ttf
* font_bold.ttf

> Important: Make sure you use the same filename for each of them.

##### Changing app colors
There are two steps to change the app's color palette
1. Add your own set of colors in [`values/palette.xml`]()
   Example:
    ```
        <color name="flamingo">#fc8eac</color>
        <color name="burlywood">#deb887</color>
        ...
    ```
2. Modify the colors in [`values/colors.xml`]()
   Example:
    ```
        <color name="brand">@color/flamingo</color>
        <color name="surface">@color/burlywood</color>
        ...
    ```
   > Tip: Make sure to modify the values in [`values/night/colors.xml`]() to support dark mode.
   

