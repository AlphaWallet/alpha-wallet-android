// Stub for tokenscript listener
const tokenscript = {
    on: (event, callback) => {
        // You can call the callback with a mock status for testing
        console.log(`Listener added for event: ${event}`);

        // Example of triggering the callback with a mock status
        // You can remove this line or modify it as needed
        callback({ status: "started" }); // Change this to test different statuses
    }
};
