## Theming
The attributes defined in [`values/themes.xml`]() exist for two primary reasons:
* Provide default color values for components -
  For example, the status bar color is derived from `colorPrimaryDark`. Meanwhile, the activated state of a checkbox is derived from `colorControlActivated`. By having these default values specified in the theme, developers no longer need to manually style most native components they want to use, effectively reducing development time and improving UX consistency.

* Allow flexibility for when theme overlays are used -
  If colors are derived from these references, we can easily apply a Dark theme overlay over a specific layout even if the application is set to Light mode.

## Styles and Typography
Along with the color references in `themes.xml`, the resources defined in [`values/styles.xml`]() and [`values/typography.xml`]() allow developers to quickly new build layouts by accessing predefined styles for the most commonly used components.

Example 1: Creating a label
```
    <TextView 
        style="@style/Aw.Typography.Label"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
```

Example 2: Creating a separator
```
    <View style="@style/Aw.Component.Separator" />
```

Example 3: Creating a secondary button
```
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
    ```
    <style name="Aw.Component.NewComponent">
        <item name="android:background">?colorPrimary</item>
        <item name="android:textColor">?colorOnPrimary</item>
    </style>
    ```

2. **Background drawables**

   You can use these color references directly onto your background drawables.

   Example:
    ```
    <shape android:shape="rectangle">
        <solid android:color="?colorSurface" />
    </shape>
    ```

3. **Directly onto a view (discouraged)**

   Before you do this, consider creating a new style instead, especially if you think that it could be reusable in other layouts.

   Example:
    ```
    <TextView
        android:textColor="?colorPrimary" 
        ... />
    ```

4. **Programmatically (discouraged)**

   Programmatically creating views often signal an inefficiency, but there are a few instances where this is unavoidable. There are two ways to access color references:

   * `MaterialColors.getColor()` for material colors (ex. `colorSurface`, etc).
   * `Context.getTheme().resolveAttribute()` for the everything else.

## Rebranding or customizing the theme
AlphaWallet is one of the best Ethereum wallets to fork, so we have provided an easy way for you to customize it.

##### Fonts
In `app/src/main/res/font`, simply replace the following files with your preferred font:

* font_light.ttf
* font_regular.ttf
* font_semibold.ttf
* font_bold.ttf

Make sure you use the same filename for each of them.

##### Colors
There are two steps to change the app's color palette
1. Add your own set of colors in [`values/brand_colors.xml`]()
   Example:
    ```
        <color name="flamingo">#fc8eac</color>
        <color name="burlywood">#deb887</color>
        ...
    ```
2. Modify the colors in [`values/palette.xml`]()
   Example:
    ```
        <color name="brand">@color/flamingo</color>
        <color name="surface">@color/burlywood</color>
        ...
    ```
   Make sure to modify the values in [`values/night/palette.xml`]() to support dark mode.

3. (Optional) Modify the values defined in `colors.xml` and `styles.xml` if you want to change the colors of specific components.

