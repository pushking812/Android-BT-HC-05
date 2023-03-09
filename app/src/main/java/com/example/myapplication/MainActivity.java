package com.example.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    // MY_UUID: UUID приложения, используемый для установки соединения по протоколу RFCOMM
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // DEVICE_ADDRESS: MAC-адрес удаленного Bluetooth-устройства, с которым устанавливается соединение
    private static final String DEVICE_ADDRESS = "98:DA:50:01:B2:53"; // здесь нужно указать адрес своего модуля HC-05

    // bluetoothAdapter: объект Bluetooth адаптера, который используется для работы с
    // Bluetooth-устройствами
    private BluetoothAdapter bluetoothAdapter;

    // bluetoothSocket: сокет Bluetooth-соединения, установленного с удаленным устройством
    private BluetoothSocket bluetoothSocket;

    // outputStream: выходной поток сокета, используется для отправки данных на удаленное устройство
    private OutputStream outputStream;

    // textView: текстовое поле для вывода сообщений о статусе соединения с удаленным устройством
    private TextView textView;
    private EditText editText;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Устанавливаем макет для активности
        setContentView(R.layout.activity_main);

        // Находим элементы интерфейса по идентификаторам
        Button sendButton = findViewById(R.id.button);
        final EditText editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textView);

        // Устанавливаем обработчик нажатия кнопки sendButton
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Получаем текст из поля ввода editText и передаем его в метод sendMessage
                sendMessage(editText.getText().toString());
                editText.setText("");
            }
        });

        try {
            // Получаем объект BluetoothAdapter для этого устройства
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                // Если Bluetooth не поддерживается на этом устройстве, выводим сообщение и выходим
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                // Если Bluetooth выключен, выводим запрос на включение Bluetooth
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            } else {
                // Если Bluetooth уже включен, пытаемся подключиться к устройству
                connectToBluetoothDevice();
            }
        } catch (Exception e) {
            // Если произошла ошибка при работе с Bluetooth, выводим сообщение с ошибкой
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /*onActivityResult() - это метод обратного вызова, который вызывается, когда ответ приходит из
    другой активности (в данном случае активности включения Bluetooth). Он вызывается после завершения
    активности и передает результат обратно в основную активность, которая вызвала эту активность.*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Если requestCode не равен REQUEST_ENABLE_BT, значит это не наш запрос, сообщение об ошибке
        if (requestCode == REQUEST_ENABLE_BT) {
            // Проверяем результат, который был передан нам из активности включения Bluetooth
            // Если он равен RESULT_OK, значит Bluetooth включен
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth включен", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth не включен", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Ошибка включения Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    // Конфигурирует Bluetooth адаптер и устанавливает соединение с удаленным Bluetooth-устройством.
    private void connectToBluetoothDevice() {
        // Проверяем, что Bluetooth адаптер существует
        if (bluetoothAdapter != null) {
            // Получаем удаленное устройство по его MAC-адресу
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
            try {
                // Создаем сокет, соответствующий протоколу RFCOMM и указываем UUID нашего приложения
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                // Устанавливаем соединение с удаленным устройством
                bluetoothSocket.connect();
                // Получаем выходной поток сокета, для отправки данных на удаленное устройство
                outputStream = bluetoothSocket.getOutputStream();
                // Если выходной поток не равен null, значит соединение установлено успешно
                if (outputStream != null) {
                    textView.setText("Connected to Bluetooth device");
                } else {
                    textView.setText("Not connected to Bluetooth device");
                }
            } catch (IOException e) {
                // Произошла ошибка при установке соединения, выводим сообщение
                Log.e(TAG, "Error connecting to Bluetooth device: " + e.getMessage());
                textView.setText("Failed to connect to Bluetooth device");
            }
        }
    }

    // Код метода sendMessage() отправляет сообщение по Bluetooth соединению на подключенное устройство.
    private void sendMessage(String message) {
        // Проверяем, установлено ли соединение с Bluetooth устройством и доступен ли
        // outputStream для отправки сообщения
        if (outputStream != null) {
            // Проверяем, что введенное сообщение не является пустым
            if (!TextUtils.isEmpty(message)) {
                // Если сообщение не пустое и Bluetooth соединение установлено, мы преобразуем сообщение в массив
                // байтов и отправляем через outputStream методом write().
                try {
                    // Преобразуем сообщение в массив байтов и отправляем через outputStream
                    outputStream.write(message.getBytes());
                    textView.setText("Sent message: " + message);
                } catch (IOException e) {
                    Log.e(TAG, "Error sending message: " + e.getMessage());
                    textView.setText("Failed to send message");
                }
            } else {
                // Выводим сообщение, если введенное сообщение пустое
                textView.setText("Message cannot be empty");
            }
        } else {
            // Выводим сообщение, если не установлено Bluetooth соединение
            textView.setText("Not connected to Bluetooth device");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Bluetooth socket: " + e.getMessage());
            }
        }
    }

}