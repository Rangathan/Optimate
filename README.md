# Business Management App

Welcome to our Business Management App! This Android application, developed in Kotlin, provides a comprehensive solution for managing your business efficiently. With features ranging from employee management to financial tracking, 
our app aims to streamline your business operations.

<img src="https://github.com/masamoe/Optimate/assets/121978255/512c4768-839c-41c7-89fe-9284e01eed3e" width="200px" height="200px">

## Team

- **Ali**  Role: Developer & Project Manager

    Responsibilities: Led XML layout design, implemented frontend logic (XML/Kotlin), resolved design issues, and managed project progress.

    Impact: Played a key role in designing user-friendly layouts, ensuring seamless frontend, and overseeing timely delivery.

- **Hison** Role: Developer
  
    Responsibilities: Develop and maintain the UI and functionality for various activities ensuring integration with Firebase for data management and user authentication.

    Impact: Enhance user experience and streamline business operations by providing intuitive interfaces and functionalities for managing user accounts, financial transactions, work logs, and payroll activities while leveraging Firebase for efficient data storage and retrieval.
      
- **Jake** Role: Developer
  
   Responsibilities: Designed and developed the user interface and user experience components, particularly in the topbar, landing page and schedule views.
  
   Impact: Primary development on the business schedule, add shift activity, and employee schedule flow including employee availability.

- **Adam**  Role: Developer

   Responsibilities: Executed backend functionalities with seamless database integration, crafted messaging systems, and orchestrated Payment Server flow.

   Impact: Made substantial contributions by deploying crucial backend functionalities and server flows, adeptly bridging the frontend and backend through logical connections.

## Tech Stack

The app is built using the following technologies:

- Kotlin
- Firebase Authentication
- Firebase Realtime Database
- Firebase Storage
- Firebase Messaging Services
- Stripe Payment
- Kotlin Coroutines
- Jetpack Compose
- Material Components for Android
- Node.js Server For Stripe Payments and Firebase Messaging 

## Contributions

We welcome contributions from developers to improve our app! Here's how you can contribute:

1. Fork the repository.
2. Make your changes in a new branch.
3. Test your changes thoroughly.
4. Submit a pull request detailing your modifications.

## Images and Videos of the App
1. Clock In and Out System
   
https://github.com/masamoe/Optimate/assets/121978255/81f7aaeb-e5f2-4260-9d60-164053726142

2. Dynamic Landing Page
   
<img src="https://github.com/masamoe/Optimate/assets/121978255/1a0a0fe0-0dfd-451e-a472-cd2077f3f466" width="200px">  <img src="https://github.com/masamoe/Optimate/assets/121978255/8d75fe65-81ad-43fe-9ab3-252613f670d6" width="200px"> <img src="https://github.com/masamoe/Optimate/assets/121978255/17b63da9-1a0e-44f5-b47a-0b78691e4e8a" width="200px">

3. Expense Uploading and History
   
<img src="https://github.com/masamoe/Optimate/assets/121978255/1998271d-a0a5-4e83-a5e7-6b12e92966bb" width="200px"> <img src="https://github.com/masamoe/Optimate/assets/121978255/4984cd0b-ddc6-422d-ab22-6768f0c18cc2" width="200px"> <img src="https://github.com/masamoe/Optimate/assets/121978255/6bce8317-5901-492b-9f92-4169a1d5ae4f" width="200px">

4. Shift Scheduling (Employee)
   
<img src="https://github.com/masamoe/Optimate/assets/121978255/9bb1e0cf-20bb-4ab5-b274-14e57e028776" width="200px">  <img src="https://github.com/masamoe/Optimate/assets/121978255/b2dadf99-3f6e-4313-bb37-ca7a1bd2c05a" width="200px"> <img src="https://github.com/masamoe/Optimate/assets/121978255/70435bc9-9a0c-45e7-9d7b-b253e929a253" width="200px"> <img src="https://github.com/masamoe/Optimate/assets/121978255/82097f68-8d15-4a7b-a540-6c84fa49457f" width="200px">

(Manager)

<img src="https://github.com/masamoe/Optimate/assets/121978255/dfef054c-1a03-475f-b4b2-5c2546a93b6f" width="200px"> <img src="https://github.com/masamoe/Optimate/assets/121978255/6a47ca33-87a2-4603-be36-31de0eca45bc" width="200px">
<img src="https://github.com/masamoe/Optimate/assets/121978255/c17cd147-7d55-4916-a466-c33596fb88f4" width="200px">

5. Employee Creation and Access
   
<img src="https://github.com/masamoe/Optimate/assets/121978255/f15ac289-11ea-4537-ba1e-aaf16edceece" width="200px"> <img src="https://github.com/masamoe/Optimate/assets/121978255/0f369f9a-7bb5-46cb-9b54-3603cfa84dfe" width="200px"> <img src="https://github.com/masamoe/Optimate/assets/121978255/44e15771-a64b-468a-96fc-39789eb6b2ec" width="200px">

# DataBase Structure for Firebase Firestore
   ```
CREATE COLLECTION accountPayment (
    docRef DOCUMENT (
        BID STRING,
        Date TIMESTAMP,
        Payment FLOAT,
        stripeId STRING
    )
);

CREATE COLLECTION availability (
    docRef DOCUMENT (
        BID STRING,
        UID STRING,
        availability ARRAY<MAP<STRING, VALUE>>,
        name STRING
    )
);

CREATE COLLECTION expenseRequest (
    docRef DOCUMENT (
        amount FLOAT,
        bid STRING,
        dateOfRequest TIMESTAMP,
        expenseRequest STRING,
        name STRING,
        reason STRING,
        receiptPhoto STRING,
        status STRING,
        uid STRING
    )
);

CREATE COLLECTION finances (
    docRef DOCUMENT (
        BID STRING,
        Expenses ARRAY<STRUCT<
            Amount FLOAT,
            Approval BOOLEAN,
            Description STRING,
            Name STRING,
            UID STRING,
            Uploaded_Date TIMESTAMP
        >>
    )
);

CREATE COLLECTION schedule (
    docRef DOCUMENT (
        BID STRING,
        day STRING,
        employees ARRAY<STRING>,
        endTime TIMESTAMP,
        startTime TIMESTAMP
    )
);

CREATE COLLECTION timeOfRequest (
    docRef DOCUMENT (
        bid STRING,
        dateOfRequest TIMESTAMP,
        endDate TIMESTAMP,
        endTime TIMESTAMP,
        name STRING,
        startTime TIMESTAMP,
        status STRING,
        uid STRING
    )
);

CREATE COLLECTION titles (
    docRef DOCUMENT (
        access ARRAY<STRING>,
        bid STRING,
        role STRING,
        title STRING
    )
);

CREATE COLLECTION totalHours (
    docRef DOCUMENT (
        UID ARRAY<STRUCT<
            time FLOAT,
            approval BOOLEAN,
            wage FLOAT
        >>,
        bid STRING
    )
);

CREATE COLLECTION users (
    docRef DOCUMENT (
        UID STRING,
        BID STRING,
        accountStatus ARRAY<STRUCT<
            date TIMESTAMP,
            status STRING
        >>,
        deviceToken STRING,
        email STRING,
        initial_password STRING,
        modules ARRAY<STRING>,
        name STRING,
        role STRING,
        title STRING,
        wage FLOAT,
        phone STRING,
        profilePic STRING,
        address STRING
    )
);

CREATE COLLECTION workLogs (
    docRef DOCUMENT (
        date ARRAY<STRUCT<
            clockIn TIMESTAMP,
            breakStart TIMESTAMP,
            breakEnd TIMESTAMP,
            clockOut TIMESTAMP
        >>,
        UID STRING,
        BID STRING
    )
);

   ```
## FireStore Storage Locations

```
images/
profileImages/{UID}
```
## Running the App

To run the app locally, follow these steps:

1. Clone the repository:

    ```
    git clone https://github.com/masamoe/Optimate.git
    ```

2. Open the project in Android Studio.
3. Add in your google-services.json from Firebase when it configured for Android.
4. Configure Firebase for authentication, database, and storage. Refer to Firebase documentation for detailed instructions.
6. Clone the node.js server which the code is located at https://github.com/Rangathan/OptimateServer
7. Replace the server url with your server url
8. Build and run the app on an Android device or emulator.

Thank you for using our Business Management App! We appreciate your support and contributions. If you encounter any issues or have suggestions for improvement, please don't hesitate to reach out to us.
