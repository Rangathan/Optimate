package com.example.optimate.loginAndRegister

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optimate.R
import com.example.optimate.businessOwner.AccountsActivity
import com.example.optimate.businessOwner.FinancesActivity
import com.example.optimate.businessOwner.Requests
import com.example.optimate.businessOwner.SchedulerActivity
import com.example.optimate.businessOwner.TitlesActivity
import com.example.optimate.employeeFlow.Availability
import com.example.optimate.employeeFlow.ClockModule
import com.example.optimate.employeeFlow.PayStub
import com.example.optimate.employeeFlow.ScheduleModule
import com.example.optimate.employeeFlow.SubmitExpenses

private val managerAccessList = listOf(
    "Scheduling",
    "Finances",
    "View Employees",
    "Time-off Requests Approval"
)

private val employeeAccessList = listOf(
    "View Schedule",
    "Clock-in/out",
    "View Payroll",
    "Add Expense",
    "Availability",

)

private val businessOwnerAccessList= listOf(
    "Titles",
    "Accounts",
    "Finances",
    "Scheduling",
    "Schedule",
    "Requests",
)
private val businessOwnerBasicAccessList= listOf(
    "Titles",
    "Accounts",
    "Scheduling",
    "Schedule",
    "Requests",
)
@Composable
fun DynamicLandingScreen(accessList: List<String>, title: String, modules: List<String>) {
    Scaffold(
        content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {  // Apply the padding to the Box
                if (title == "businessOwner" && modules.contains("Plus")) {
                    ButtonList(AccessList = businessOwnerAccessList)
                    Log.d("compose", "access: $accessList")
                }
                else if (title == "businessOwner") {
                    ButtonList(AccessList = businessOwnerBasicAccessList)
                    Log.d("compose", "access: $accessList")
                }
                else if (title == "Employee") {
                    ButtonList(AccessList = accessList)
                    Log.d("compose", "access: $accessList")
                } else {
                    ButtonList(AccessList = accessList)
                    Log.d("compose", "access: $accessList")
                }
            }
        }
    )
}

@Composable
fun ButtonList(AccessList: List<String>) {
    val context = LocalContext.current

    val modifiedList = AccessList.filter { item ->
        item != "Request Time-off"
    }
    Box(
        //contentAlignment = Alignment.Center, // This centers its children
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.screen_border_colors))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
            modifier = Modifier.fillMaxWidth() // You might adjust this for better centering, depending on your design
        ) {
            itemsIndexed(modifiedList) { index, item ->
                EachButton(
                    text = item,
                    onClick = { when (item) {
                        "Titles" -> context.startActivity(Intent(context, TitlesActivity::class.java))
                        "Accounts" -> context.startActivity(Intent(context, AccountsActivity::class.java))
                        "Finances" -> context.startActivity(Intent(context, FinancesActivity::class.java))
                        "Clock-in/out" -> context.startActivity(Intent(context, ClockModule::class.java))
                        "Schedule" -> context.startActivity(Intent(context, ScheduleModule::class.java))
                        "Scheduling" -> context.startActivity(Intent(context, SchedulerActivity::class.java))
                        "Requests" -> context.startActivity(Intent(context, Requests::class.java))
                        "Time-off Requests Approval" -> context.startActivity(Intent(context, Requests::class.java))
                        "View Employees" -> context.startActivity(Intent(context, AccountsActivity::class.java))
                        "View Schedule" -> context.startActivity(Intent(context, ScheduleModule::class.java))
                        "View Payroll" -> context.startActivity(Intent(context, PayStub::class.java))
                        "Availability" -> context.startActivity(Intent(context, Availability::class.java))
                        "Add Expense" -> context.startActivity(Intent(context, SubmitExpenses::class.java))



                    }},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

}


@Composable
fun EachButton(text: String, onClick: () -> Unit, modifier: Modifier, ) {

    val buttonImg = when (text) {
        "View Schedule" -> painterResource(id =R.drawable.ic_schedule_foreground)
        "Clock-in/out" -> painterResource(id =R.drawable.clock)
        "View Payroll" -> painterResource(id =R.drawable.payroll)
        "Titles" -> painterResource(id =R.drawable.accounts)
        "Accounts" -> painterResource(id =R.drawable.ic_roles_foreground)
        "Finances" -> painterResource(id =R.drawable.ic_finances_foreground)
        "Schedule" -> painterResource(id =R.drawable.ic_schedule_foreground)
        "Scheduling" -> painterResource(id =R.drawable.scheduling)
        "Requests" -> painterResource(id =R.drawable.ic_requests_foreground)
        "View Employees" -> painterResource(id =R.drawable.ic_roles_foreground)
        "Time-off Requests Approval" -> painterResource(id =R.drawable.ic_requests_foreground)
        "Availability" -> painterResource(id = R.drawable.availability)
        "Add Expense" -> painterResource(id =R.drawable.expense_icon)

        else -> painterResource(id =R.drawable.ic_requests_foreground)
    }
    val buttonText = when(text) {
        "Scheduling" -> "Scheduler"
        "View Employees" -> "Employees"
        "View Schedule" -> "Schedule"
        "Clock-in/out" -> "Clock-In/Out"
        "View Payroll" -> "Payroll"
        "Time-off Requests Approval" -> "Requests"
        "Add Expense" -> "Submit Expense"
        "Availability" -> "Availability"
        else -> text
    }
    val buttonColor = when (text) {
        "View Schedule" -> colorResource(id = R.color.purple_button)
        "Clock-in/out" -> colorResource(id = R.color.gray_button)
        "View Payroll" -> colorResource(id = R.color.green_button)
        "Titles" -> colorResource(id = R.color.purple_button)
        "Accounts" -> colorResource(id = R.color.blue_button)
        "Finances" -> colorResource(id = R.color.green_button)
        "Schedule" -> colorResource(id = R.color.purple_button)
        "Scheduling" -> colorResource(id = R.color.red_button)
        "Requests" -> colorResource(id = R.color.red_button)
        "View Employees" -> colorResource(id = R.color.blue_button)
        "Availability" -> colorResource(id = R.color.blue_button)
        "Time-off Requests Approval" -> colorResource(id = R.color.red_button)
        "Add Expense" -> colorResource(id = R.color.red_button)


        else -> Color(0xFF90A4AE)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onClick,
            modifier = modifier
                .wrapContentSize()
                .height(90.dp)
                .width(90.dp),
            //.border(3.dp, FrameColor, shape = RoundedCornerShape(15.dp)),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 16.dp),
            contentPadding = PaddingValues(15.dp)

        ) {

            Icon(
                painter = buttonImg,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = buttonColor
            )

        }
        Text(
            text = buttonText,
            modifier = Modifier.padding(top = 6.dp),
            fontSize = 18.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }

}

@Preview
@Composable
fun BusinessOwnerButtonListPreview() {
    DynamicLandingScreen(managerAccessList, "businessOwner", GlobalUserData.modules)
}

