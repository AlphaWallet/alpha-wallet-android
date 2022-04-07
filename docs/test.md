## E2E test
### Quick Start
1. Create a test class under source set `androidTest` and extends `BaseE2ETest`
```Java
package com.alphawallet.app;

public class ManageNetworkTest extends BaseE2ETest
{
}
```

2. Generate JUnit test method
```Java
@Test
public void should_add_custom_network()
{
}
```

3. Add steps
```Java
Steps.createNewWallet();
Steps.gotoSettingsPage();
```

4. Create your steps

### Concepts
__Operation__
Provided by Espresso, user click, press back button, type text in, etc.
>onView(withId(R.id.some_id)).perform(ViewActions.click());

Click list item:
```Java
// list id: R.id.test_list
// item index: 3
onView(withId(R.id.test_list)).perform(actionOnItemAtPosition(3, ViewActions.click()));
```

Find all view matchers here: https://developer.android.com/reference/androidx/test/espresso/matcher/ViewMatchers#summary

__Step__
One or more user operations with a business intent: `createNewWallet`, `sendBalanceTo`
Steps are put in class `Steps`, so can be reuse in different test cases.

### Utilities
Dependency hierarchy: 
>JUnit test method -> Steps -> Helpers

Click item with id:
>click(withId(R.id.xxx));

Click button with text:
>click(withText("Add"));
 
Wait n seconds;
>Helper.wait(n);

You can check `Helper` to see all methods.

### Assertions
Check text displayed on screen:
>shouldSee("Some text");

Feel free to add more useful assertions into class `Should`.

### Debug
If test fails, it will take screenshot automatically. So you can see what really happened.
You can also take screenshots by specifying a file name:
>SnapshotUtil.take("file-name");

After GitHub action jobs done, you can download the screenshots and logcat from the artifacts.
