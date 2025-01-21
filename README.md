# EmployeeApplicationWithEmailVerification

Features of the Employee Management Application:
User Features:
Account Creation with Email Verification

Users receive an email for verification upon creating an account.
Clicking the verification link activates the account, allowing the user to log in.
Forgot Password Functionality

Users can reset their password by entering their email.
A reset link is sent via email.
Employee Dashboard (Post Login):

Update Personal Details: Employees can update their personal information.
View Reviews and Ratings: Employees can view feedback and ratings provided by their manager.
Apply for Leave: Employees can submit leave requests.
View balance leaves 
Logout Option: Users can securely log out of their accounts.
Admin/Manager Features:
Employee List Page:

Displays all employees in a paginated format.
Includes a search bar for finding employees quickly.
Export to CSV: Exports all employee details to a CSV file.
Add New Employee: Allows admins to add new employees to the system.
Employee Details Management:

Admins can edit employee details.
Admins can assign ratings and provide reviews.
Admins can delete an employee record.
Leave Management:

View Leave Requests Tab: Admins can view, approve, or reject employee leave requests.
Activity Status Tab:

Admins can monitor activities performed in an employee's account.
Technologies Used:
Backend Development:

Java
Spring Boot
Frontend Development:

Thymeleaf
HTML
CSS
JavaScript
Database:

MySQL (or similar RDBMS for storing employee and admin data)
Email Functionality:

SMTP (for sending email notifications)
File Export:

CSV generation for exporting employee details
Other Tools and Features:

Pagination for employee lists
Search functionality
Authentication and authorization with Spring Security
