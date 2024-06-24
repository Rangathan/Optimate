package com.example.optimate.businessOwner
import android.app.DatePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Card
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optimate.R
import com.example.optimate.employeeFlow.NoDataFound
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Finances(val type: String, val amount: String, val date: String, val description: String, val name: String)
@Composable
fun FinancesDetailScreen(finances: List<Finances>) {
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var filteredFinances by remember { mutableStateOf(finances) }

    Scaffold(
        topBar = { XmlTopBar(titleText = "Finances Details") },
        containerColor = colorResource(id = R.color.screen_border_colors),
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    DateInput(label = "From", date = fromDate, onDateChange = { fromDate = it }, modifier = Modifier.weight(1f))
                    DateInput(label = "To", date = toDate, onDateChange = { toDate = it }, modifier = Modifier.weight(1f))
                    SearchDate(fromDate, toDate, finances) { filteredFinances = it }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if(finances.isEmpty()){
                    NoDataFound("No data found")
                }
                else{
                    FinancesList(finances = filteredFinances)
                }

            }
        }
    )
}
@Composable
fun SearchDate(
    fromDate: String,
    toDate: String,
    finances: List<Finances>,
    onFilterResult: (List<Finances>) -> Unit
) {
    val buttonColor = colors.run { colorResource(id = R.color.light_green)}
    Button(
        onClick = {
            if (fromDate.isNotEmpty() && toDate.isNotEmpty()) {
                val format = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                val fromDateParsed = format.parse(fromDate)
                val toDateParsed = format.parse(toDate)

                val filteredFinances = if (fromDateParsed != null && toDateParsed != null) {
                    finances.filter { finance ->
                        val financeDate = format.parse(finance.date)
                        financeDate != null && !financeDate.before(fromDateParsed) && !financeDate.after(toDateParsed)
                    }
                } else {
                    finances // If parsing fails, return the original list
                }
                onFilterResult(filteredFinances) // Pass the filtered list back to the caller
            }
        },
        modifier = Modifier
            .padding(top = 20.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
            .height(36.dp),
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)



    ) {
        Text("Search",fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FinancesList(finances: List<Finances>) {
    LazyColumn {
        itemsIndexed(finances) { _, finance ->
            FinancesRow(finance)
        }
    }
}
@Composable
fun FinancesRow(finance: Finances) {
    var isExpanded by remember { mutableStateOf(false) }

    // Decide the card color based on the type
    val revenueColor = colors.run { colorResource(id = R.color.light_green)}
    val expenseColor = colors.run { colorResource(id = R.color.light_red)}
    val cardColor = if (finance.type == "Expenses") expenseColor else revenueColor
    val cornerRadius = 12.dp

    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }// Toggle isExpanded on click
            .border(2.dp, cardColor, shape = RoundedCornerShape(cornerRadius)),

        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(cornerRadius),
    ) {
        Column(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth()
        ) {
            Text(text = " " , fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                if(finance.type =="Expenses"){
                    Text(text = "-$" + finance.amount, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                else{
                    Text(text = "+$" + finance.amount, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Row {
                    Text(text = finance.date, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.clickable { isExpanded = !isExpanded }
                    )
                }
            }
            Text(text = " " , fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

            // Expandable content
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                ){
                    Text(text = "'" + finance.description + "'", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.DarkGray)
                    Text(text = "From: " + finance.name, fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.DarkGray)
                }
            }
        }
    }
}

@Composable
fun DateInput(label: String, date: String, onDateChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = date,
        onValueChange = { },
        modifier = modifier
            .padding(4.dp),
        readOnly = true,
        textStyle = TextStyle(fontSize = 10.sp),
        label = { Text(text = label, fontSize = 12.sp) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Date Picker",
                modifier = Modifier.clickable { showDialog = true }
            )
        }
    )

    if (showDialog) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, dayOfMonth ->
                // Ensure month and day are two digits
                val formattedMonth = (selectedMonth + 1).toString().padStart(2, '0')
                val formattedDay = dayOfMonth.toString().padStart(2, '0')
                // Format the date in MM/dd/yyyy format
                val newDate = "$formattedMonth/$formattedDay/$selectedYear"
                onDateChange(newDate)
                showDialog = false
            },
            year,
            month,
            day
        ).apply {
            setOnDismissListener { showDialog = false }
            show()
        }
    }
}
@Preview
@Composable
fun FinancesDetailScreenPreview() {
    FinancesDetailScreen(
        finances = listOf(
            Finances("Revenues", "9.99", "2022-10-10", "This is a revenue", "John Doe"),
            Finances("Expenses", "50", "2022-10-10", "This is an expense", "John Doe"),
            Finances("Revenues", "100", "2022-10-10", "This is a revenue", "John Doe"),
            Finances("Expenses", "50", "2022-10-10", "This is an expense", "John Doe"),
            Finances("Revenues", "100", "2022-10-10", "This is a revenue", "John Doe"),
            Finances("Expenses", "50", "2022-10-10", "This is an expense", "John Doe"),
            Finances("Revenues", "100", "2022-10-10", "This is a revenue", "John Doe"),
        )
    )
}