package com.example.optimate.businessOwner

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optimate.R

@Composable
fun FinancesScreen(revenues: Double, expenses: Double, amountWithDate: List<FinancesActivity.AmountWithDate>) {
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var filteredRevenues by remember { mutableDoubleStateOf(revenues) }
    var filteredExpenses by remember { mutableDoubleStateOf(expenses) }
    var filteredAmountWithDate by remember { mutableStateOf(amountWithDate) }
    var donutChartFromDate by remember { mutableStateOf("") }
    var donutChartToDate by remember { mutableStateOf("") }

    Scaffold(
        topBar = { XmlTopBar(titleText = "Finances") },
        containerColor = colorResource(id = R.color.screen_border_colors),
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly

                ) {
                    AddRevenue()
                    AddExpense()

                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DateInput(
                        label = "From",
                        date = fromDate,
                        onDateChange = { fromDate = it },
                        modifier = Modifier.weight(1f)
                    )
                    DateInput(
                        label = "To",
                        date = toDate,
                        onDateChange = { toDate = it },
                        modifier = Modifier.weight(1f)
                    )
                    Search(
                        fromDate,
                        toDate,
                        amountWithDate
                    ) { revenues, expenses, filteredFinances, from, to ->
                        filteredRevenues = revenues
                        filteredExpenses = expenses
                        filteredAmountWithDate = filteredFinances
                        donutChartFromDate = from
                        donutChartToDate = to

                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                DonutChart(filteredRevenues, filteredExpenses, donutChartFromDate, donutChartToDate)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PayStubs()
                    ViewMore()
                }
            }
        }
    )
}

@Composable
fun AddRevenue() {
    val buttonColor = colors.run { colorResource(id = R.color.light_green) }
    val addRevenueBtn = LocalContext.current

    // Add Revenue button
    Button(
        onClick = {
            val intent = Intent(addRevenueBtn, AddRevenueOrExpenseActivity::class.java).apply {
                putExtra("type", "Revenue")
            }
            addRevenueBtn.startActivity(intent)
        },
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Text("Add Revenue", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun AddExpense() {
    val buttonColor = colors.run { colorResource(id = R.color.light_red)}
    val addExpenseBtn = LocalContext.current

    // Add Expense button
    Button(
        onClick = {
            val intent = Intent(addExpenseBtn, AddRevenueOrExpenseActivity::class.java).apply {
                putExtra("type", "Expense")
            }
            addExpenseBtn.startActivity(intent)
        },
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)

    ) {
        Text("Add Expense", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun Search(
    from: String,
    to: String,
    amountWithDate: List<FinancesActivity.AmountWithDate>,
    onFilterResult: (Double, Double, List<FinancesActivity.AmountWithDate>, String, String) -> Unit
) {
    val buttonColor = colorResource(id = R.color.light_green)
    Button(
        onClick = {
            if (from.isNotEmpty() && to.isNotEmpty()) {
                val format = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                val fromDateParsed = format.parse(from)
                val toDateParsed = format.parse(to)

                val filteredFinances = if (fromDateParsed != null && toDateParsed != null) {
                    amountWithDate.filter { finance ->
                        val financeDate = format.parse(finance.date)
                        financeDate != null && !financeDate.before(fromDateParsed) && !financeDate.after(toDateParsed)
                    }
                } else {
                    amountWithDate
                }

                val expenses = filteredFinances
                    .filter { it.type == "Expenses" }
                    .sumOf { it.amount.toDouble() }

                val revenues = filteredFinances
                    .filter { it.type == "Revenues" }
                    .sumOf { it.amount.toDouble() }

                onFilterResult(revenues, expenses, filteredFinances, from, to)
            }
        },
        modifier = Modifier
            .padding(top = 20.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
            .height(36.dp),
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Text("Search", fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PayStubs() {
    val buttonColor = colorResource(id = R.color.light_green)
    val payStubsBtn = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(payStubsBtn, PayStubsActivity::class.java)
            payStubsBtn.startActivity(intent)
        },
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Text("Pay-stubs", fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
    }
}


@Composable
fun ViewMore(){
    val viewMoreBtn = LocalContext.current
    val buttonColor = colorResource(id = R.color.light_green)
    Button(
        onClick = {
            val intent = Intent(viewMoreBtn, FinancesDetailActivity::class.java)
            viewMoreBtn.startActivity(intent)
        },
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Text("View More", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}


@Composable
fun DonutChart(revenues: Double, expenses: Double, from: String = "", to: String = "") {
    val total = if (revenues == 0.0 && expenses == 0.0) 1.0 else revenues + expenses // Avoid division by zero
    val revenueAngle = if (revenues == 0.0 && expenses == 0.0) 360f else (revenues / total * 360).toFloat()
    val expensesAngle = if (revenues == 0.0 && expenses == 0.0) 0f else 360f - revenueAngle
    val revenueColor = if (revenues == 0.0 && expenses == 0.0) Color.Gray else colorResource(id = R.color.light_green)
    val expenseColor = if (revenues == 0.0 && expenses == 0.0) Color.Gray else colorResource(id = R.color.light_red)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(350.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Box(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
            if (from.isEmpty() && to.isEmpty()) {
                Text("Finances", color = colors.onSurface, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
            } else {
                Text("$from - $to", color = colors.onSurface, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Revenue: $${String.format("%.2f", revenues)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp), color = Color.DarkGray)
                Text("Expenses: $${String.format("%.2f", expenses)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp), color = Color.DarkGray)
                Text("Balance: $${String.format("%.2f", revenues - expenses)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Canvas(modifier = Modifier.size(200.dp)) { // Set this to your desired size
                    val strokeWidth = 50f
                    val radius = size.minDimension / 2 - strokeWidth / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw the arc for revenues or the full grey donut if both are 0
                    drawArc(
                        color = revenueColor,
                        startAngle = -90f,
                        sweepAngle = revenueAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + if (revenues == 0.0 && expenses == 0.0) 0f else 20f), // Remove the additional stroke width for the grey donut
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    // Draw the arc for expenses unless both revenues and expenses are 0
                    if (revenues != 0.0 || expenses != 0.0) {
                        drawArc(
                            color = expenseColor,
                            startAngle = -90f + revenueAngle,
                            sweepAngle = expensesAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                    }
                }
            }
        }
    }
}



@Preview
@Composable
fun FinancesScreenPreview() {
    FinancesScreen(revenues = 1000.0, expenses = 500.0, amountWithDate = emptyList())
}